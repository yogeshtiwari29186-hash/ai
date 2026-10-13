#!/usr/bin/env python3
"""Educational local-only character bigram trainer with checkpoint/resume."""
import argparse
import json
import random
import time
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATA = HERE / "data.txt"
CHECKPOINT_DIR = HERE / "checkpoints"
LOG_DIR = HERE / "logs"
CKPT = CHECKPOINT_DIR / "latest.json"
LOG = LOG_DIR / "training.log"

def log(message):
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')} {message}"
    print(line, flush=True)
    with LOG.open("a", encoding="utf-8") as f:
        f.write(line + "\n")

def generate(counts, length=240):
    chars = list(counts.keys())
    if not chars:
        return "(train the model first)"
    current = random.choice(chars)
    out = [current]
    for _ in range(length - 1):
        options = counts.get(current)
        if options:
            next_chars, weights = zip(*options.items())
            current = random.choices(next_chars, weights=weights, k=1)[0]
        else:
            current = random.choice(chars)
        out.append(current)
    return "".join(out)

def train(steps, resume):
    if not DATA.exists():
        raise SystemExit(f"Training data not found: {DATA}")
    text = DATA.read_text(encoding="utf-8").lower()
    if len(text) < 2:
        raise SystemExit("Add more training text to data.txt (at least 2 characters).")
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    counts = defaultdict(Counter)
    completed = 0
    if resume and CKPT.exists():
        state = json.loads(CKPT.read_text(encoding="utf-8"))
        counts = defaultdict(Counter, {k: Counter(v) for k, v in state.get("counts", {}).items()})
        completed = int(state.get("completed_steps", 0))
        log(f"RESUME checkpoint={CKPT.name} completed_steps={completed}")
    else:
        log("START fresh training")
    pairs = list(zip(text[:-1], text[1:]))
    random.seed(2026 + completed)
    target = completed + steps
    for step in range(completed, target):
        a, b = random.choice(pairs)
        counts[a][b] += 1
        if (step + 1) % 10 == 0 or step + 1 == target:
            state = {
                "format_version": 1,
                "completed_steps": step + 1,
                "counts": {k: dict(v) for k, v in counts.items()},
                "note": "Educational character bigram model; not a video-generation model."
            }
            tmp = CKPT.with_suffix(".tmp")
            tmp.write_text(json.dumps(state), encoding="utf-8")
            tmp.replace(CKPT)
            log(f"CHECKPOINT saved completed_steps={step + 1}")
    log(f"DONE total_steps={target} checkpoint={CKPT}")
    log("SAMPLE " + generate(counts).replace("\n", " "))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--steps", type=int, default=100, help="additional training steps")
    parser.add_argument("--resume", action="store_true", help="resume from latest checkpoint")
    args = parser.parse_args()
    if args.steps < 1:
        raise SystemExit("--steps must be >= 1")
    train(args.steps, args.resume)
