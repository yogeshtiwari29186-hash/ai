# Firebase setup

Place the Firebase `google-services.json` supplied for project `carpenter-94746` at:
- `parental-control/parent/google-services.json`
- `parental-control/child/google-services.json`

Before use, enable Email/Password in Firebase Authentication and enable Realtime Database. Configure restrictive Realtime Database Security Rules. Pairing should be moved to a trusted backend/Cloud Function that validates and consumes one-time codes. Do not use broad authenticated read/write rules in production.

Screen sharing, location, and app usage require transparent on-device permissions and Android system consent. This starter does not secretly capture screens, messages, or camera.
