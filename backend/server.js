const express = require("express");

const app = express();
app.use(express.json({ limit: "1mb" }));

const PORT = Number(process.env.PORT || 3000);
const REPLICATE_API_TOKEN = process.env.REPLICATE_API_TOKEN;
const MODEL = process.env.REPLICATE_MODEL || "prunaai/p-video";
const ALLOWED_ORIGIN = process.env.ALLOWED_ORIGIN || "*";

app.use((req, res, next) => {
  res.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  if (req.method === "OPTIONS") return res.sendStatus(204);
  next();
});

function normalizeVideoUrl(output) {
  if (typeof output === "string" && /^https:\/\//i.test(output)) return output;
  if (Array.isArray(output)) {
    for (const item of output) {
      const url = normalizeVideoUrl(item);
      if (url) return url;
    }
  }
  if (output && typeof output === "object") {
    for (const key of ["url", "video", "output", "video_url"]) {
      if (output[key]) {
        const url = normalizeVideoUrl(output[key]);
        if (url) return url;
      }
    }
  }
  return null;
}

async function replicateRequest(path, options = {}) {
  const response = await fetch(`https://api.replicate.com/v1${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${REPLICATE_API_TOKEN}`,
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    signal: AbortSignal.timeout(30000)
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = body.detail || body.title || body.error || `Replicate returned HTTP ${response.status}`;
    throw new Error(String(message).slice(0, 500));
  }
  return body;
}

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "ai-video-maker-backend", configured: Boolean(REPLICATE_API_TOKEN) });
});

app.post("/api/videos", async (req, res) => {
  if (!REPLICATE_API_TOKEN) return res.status(503).json({ error: "Server owner must configure REPLICATE_API_TOKEN." });
  const prompt = typeof req.body?.prompt === "string" ? req.body.prompt.trim() : "";
  if (prompt.length < 5) return res.status(400).json({ error: "Prompt must contain at least 5 characters." });
  if (prompt.length > 4000) return res.status(400).json({ error: "Prompt is too long (maximum 4000 characters)." });

  try {
    const prediction = await replicateRequest(`/models/${MODEL}/predictions`, {
      method: "POST",
      headers: { Prefer: "wait=1" },
      body: JSON.stringify({ input: { prompt, prompt_upsampling: false } })
    });
    res.status(202).json({
      id: prediction.id,
      status: prediction.status || "starting",
      videoUrl: normalizeVideoUrl(prediction.output),
      error: prediction.error || null
    });
  } catch (error) {
    res.status(502).json({ error: error.message || "Unable to start video generation." });
  }
});

app.get("/api/videos/:id", async (req, res) => {
  if (!REPLICATE_API_TOKEN) return res.status(503).json({ error: "Server owner must configure REPLICATE_API_TOKEN." });
  const id = String(req.params.id || "");
  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(id)) return res.status(400).json({ error: "Invalid generation ID." });

  try {
    const prediction = await replicateRequest(`/predictions/${encodeURIComponent(id)}`);
    res.json({
      id: prediction.id,
      status: prediction.status || "unknown",
      videoUrl: normalizeVideoUrl(prediction.output),
      error: prediction.error || null
    });
  } catch (error) {
    res.status(502).json({ error: error.message || "Unable to check generation status." });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`AI Video Maker backend listening on port ${PORT}`);
});
