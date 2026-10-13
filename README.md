# AI Video Maker — Android starter

A phone-first Android app for drafting a video storyboard from an idea.

## Current app features
- Enter a video idea and choose a format (vertical Shorts/Reels, widescreen YouTube, or square).
- Choose a target duration from 30 seconds to 5 minutes.
- Create a local storyboard with narration text, scene timings, visual prompts, on-screen text, and editing notes.
- Listen to the narration using Android's built-in Text-to-Speech engine.
- Share/export the plan as text to another app.

## Important limitations
This is **not yet a real text-to-video generator**. The current storyboard is assembled locally from templates; Android Text-to-Speech reads the narration but does not create a video file. There is no connected text-to-video model or external provider yet. To render actual AI-generated video, a compatible generation API/service (usually requiring an API key and potentially paid credits) must be selected and integrated securely. Never put a private provider API key directly in a public APK or public GitHub repository.

The included `training/train.py` is an educational character-level bigram trainer, **not** a video model. Training a full text-to-video model from scratch on a typical phone or ordinary GitHub Actions runner is not realistic.

## Build APK
Open the latest successful workflow at:
https://github.com/yogeshtiwari29186-hash/ai/actions
Open the newest run and download the `ai-video-maker-debug-apk` artifact. Extract the ZIP and install the APK on an Android device.

## Android project
Open `android/` in Android Studio. GitHub Actions builds a debug APK and uploads it as an artifact.
