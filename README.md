# AI Video Maker — Android app + AI video backend

Phone-first Android app for creating a storyboard and optionally generating a real video through a server-side video model API.

## Android app features
- Enter a video idea and choose vertical Shorts/Reels, widescreen YouTube, or square format.
- Choose a target duration from 30 seconds to 5 minutes.
- Create a local storyboard with narration text, scene timings, visual prompts, on-screen text, and editing notes.
- Listen to narration with Android Text-to-Speech.
- Share/export the storyboard as text.
- Enter a deployed HTTPS backend URL, start a real text-to-video generation request, poll the generation status, and download the resulting MP4 to Downloads.

## Real AI video generation setup

The app now has a server integration, but generation will only work after you deploy the backend and configure a provider token.

1. Open `backend/README.md` for setup.
2. Deploy the `backend/` directory as a Node.js service. A Render blueprint is included in `render.yaml`.
3. Add `REPLICATE_API_TOKEN` as a secret environment variable in the hosting dashboard; never add it to the Android app or GitHub source.
4. Check the current pricing and model input schema at https://replicate.com/prunaai/p-video before generating. The provider may charge per generation.
5. Copy the deployed service's HTTPS URL into the Android app's backend URL field.

## Important limitations
- The backend integration is a starter and has not been deployed from this repository. No hosted endpoint or provider token is included.
- Public production use needs real user authentication, persistent rate limits, quotas and billing protections.
- The included `training/train.py` is an educational character-level bigram trainer, **not** a video model. Training a full video model from scratch on a typical phone or ordinary GitHub Actions runner is not realistic.

## Download the Android APK
Open the latest successful workflow:
https://github.com/yogeshtiwari29186-hash/ai/actions

Open the newest completed successful run and download the `ai-video-maker-debug-apk` artifact. Extract the ZIP and install the APK on your Android device.

## Project layout
- `android/` — Android app source.
- `backend/` — server-side Replicate API bridge.
- `training/` — educational text-model training example.
- `.github/workflows/android.yml` — GitHub Actions APK build.
- `render.yaml` — backend deployment blueprint.
