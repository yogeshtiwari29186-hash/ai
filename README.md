# AI Video Maker — phone-first starter

This repository is the first local-only prototype scaffold for AI Video Maker. It makes no external AI API calls.

## Included
- `android/`: minimal Android app shell with a prompt screen.
- `training/train.py`: educational character bigram trainer with JSON checkpoints, resume support, and logs.
- `.github/workflows/android.yml`: GitHub Actions workflow to attempt a debug APK build.

## Important limitations
The included trainer is **not** a text-to-video model, and the Android screen does not yet generate a video. This is a scaffold for the next implementation milestone. Real local video generation requires a compatible model and substantial storage/RAM/compute. Training a full video model from scratch on a typical phone is not realistic.

## Run the training prototype on Android with Termux
1. Install Termux from its official F-Droid or GitHub source.
2. Run:
   ```sh
   pkg update
   pkg install python git
   git clone https://github.com/yogeshtiwari29186-hash/ai.git
   cd ai/training
   python train.py --steps 100
   ```
3. Resume with `python train.py --steps 100 --resume`.
4. Checkpoints are saved to `training/checkpoints/latest.json`; logs to `training/logs/training.log`.

The trainer runs only while the process is allowed to run. Android battery/background restrictions can stop it; GitHub stores source code but does not provide free continuous GPU training.

## Android project
Open `android/` in Android Studio. GitHub Actions attempts to build a debug APK and upload it as an artifact. The project has not yet been connected to a real video model, so do not treat the app as a finished video generator.
