# Contributing to ScanPilot

Thank you for helping improve ScanPilot — a free, open-source PDF scanner and reader for Android.

## How to contribute

1. **Fork** the repository and clone your fork.
2. Create a branch: `git checkout -b feature/your-idea` (or `fix/your-bug`).
3. Make your change. Keep the UI in English (LTR, US spelling) and the code clean and commented.
4. **Test it** in Android Studio: build the debug APK (`gradlew assembleDebug`) and run the affected screens on an emulator or a real phone.
5. Open a **pull request** with a short description: what changed, why, and how you tested it.

## Guidelines

- **Scope:** one change per PR — a feature, a bug fix, or a docs improvement.
- **No secrets:** never commit API keys, keystores, passwords or tokens. AI keys belong in the app's Settings, never in code.
- **No copied assets:** do not copy icons, screenshots or listing text from other apps (Adobe Scan, CamScanner, Genius Scan, etc.).
- **Honesty:** if a feature is a stub or untested, say so in the PR and in `BUILD_NOTES_EN.md`.
- **Docs:** if you change a user-facing feature, update `README.md` and, when relevant, `store-listing-en.md`.

## Reporting bugs

Open an [issue](https://github.com/kml11kml/Scanpilot/issues) with:

- What you did (steps to reproduce)
- What you expected vs. what happened
- Device / Android version, app version
- A screenshot or log, if you have one

## Ideas welcome

OCR, scan cropping, Word/Excel import, and real Google Play Billing wiring are on the roadmap (see `README.md`). If you want to take one on, open an issue first so work doesn't overlap.
