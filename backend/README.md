# AI Video Maker generation backend

This server keeps the Replicate token on the server and exposes a small API for the Android app. Do **not** put `REPLICATE_API_TOKEN` in Android code or commit it to GitHub.

## Deploy it

Deploy the `backend/` folder as a Node.js web service on a host that supports Node.js 20+ (for example Render or Railway).

- Build/install command: `npm install`
- Start command: `npm start`
- Health check path: `/health`
- Add environment variable `REPLICATE_API_TOKEN` in the hosting dashboard.
- Optional environment variable `REPLICATE_MODEL`, default: `prunaai/p-video`.
- Copy the service's HTTPS URL and paste it into the Android app's backend URL field.

Create a Replicate account and API token at https://replicate.com/account/api-tokens. Video generation may incur charges; check the selected model's current pricing before use: https://replicate.com/prunaai/p-video.

## API

- `GET /health` — basic service health/configuration check.
- `POST /api/videos` with JSON `{"prompt":"..."}` — start a prediction and return `id`, `status`, `videoUrl`, and `error`.
- `GET /api/videos/:id` — check prediction status and output URL.

The Android app polls status and asks Android Download Manager to save the generated MP4 under Downloads.

## Important production notes

This is an integration starter, not a fully hardened public service. Do not expose an unrestricted server using your paid token to the public. Add user authentication, usage quotas, persistent rate limiting, billing controls, and monitoring before public launch. Provider output formats or model input schemas may change; verify them against the current model API page. Generation time and availability depend on the provider.
