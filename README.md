# ScanPilot — PDF Scanner, Reader & Editor for Android

[![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Target API 36](https://img.shields.io/badge/Target%20API-36%20(Android%2016)-34A853)](https://developer.android.com/about/versions/16)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Open Source](https://img.shields.io/badge/Open%20Source-Yes-brightgreen.svg)](#contributing)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)

**Scan. Read. Summarize.**

ScanPilot is a **free, open-source PDF scanner and PDF reader for Android** that turns your phone into a pocket document scanner. **Scan documents to PDF** with your camera, **read, annotate, merge, split, sign and password-protect PDFs**, and get **AI summaries** of your documents — **offline, with no account, no ads and no tracking**.

> **Keywords:** pdf scanner · scan to pdf · pdf reader · pdf editor · document scanner · merge pdf · split pdf · sign pdf · pdf converter · image to pdf · pdf to image · ai summary · android pdf app · kotlin · jetpack compose · open source

---

## Table of Contents

- [Why ScanPilot?](#why-scanpilot)
- [Features](#features)
- [How to Scan a Document to PDF on Android](#how-to-scan-a-document-to-pdf-on-android)
- [Screenshots](#screenshots)
- [Who Is It For?](#who-is-it-for)
- [ScanPilot vs Other PDF Scanner Apps](#scanpilot-vs-other-pdf-scanner-apps)
- [Getting Started — Build the App](#getting-started--build-the-app)
- [Tech Stack](#tech-stack)
- [FAQ — PDF Scanner Questions, Answered](#faq--pdf-scanner-questions-answered)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Privacy](#privacy)
- [License](#license)

---

## Why ScanPilot?

Most PDF scanner apps on Google Play are closed-source, full of ads, and ask you to create an account before you can scan a single page. ScanPilot is different:

- **100% offline** — scanning, reading, merging and splitting work without the internet.
- **No account, no ads, no tracking** — open the app and scan in under 30 seconds.
- **Private by design** — your files stay on your phone. Only when you tap **AI Summary** is a document's text sent to the AI service *you* choose.
- **Open source (MIT)** — read the code, audit it, improve it, fork it.
- **Built for Android 16** — targets API 36, the current Google Play requirement for new apps.

## Features

| Feature | What it does |
|---|---|
| 📷 **Document scanner** | CameraX scan with a guide frame, colour / black-and-white / grayscale filters, rotate & reorder pages |
| 📄 **PDF reader** | Pinch zoom, page slider, go-to-page, night mode for comfortable reading |
| ✍️ **PDF editor & annotation** | Highlight, underline, strikethrough, draw and sign on any PDF page |
| 🔀 **Merge PDF** | Combine several PDFs into one file in a few taps |
| ✂️ **Split PDF** | Split one PDF into separate documents |
| 🖼️ **Image to PDF / PDF to image** | Convert photos to PDF and export PDF pages as images |
| 🔒 **Password-protect PDF** | Lock sensitive documents with a password (PDFBox) |
| 🤖 **AI summary** | Summarize lecture notes, contracts and reports in seconds — with your own API key |
| 🗂️ **Document library** | First-page thumbnails, search, sort, folders, favorites, rename, move, delete |
| 📤 **Share anywhere** | One-tap share via WhatsApp, Gmail, Google Drive and more |
| 🌙 **Dark mode** | Full dark theme, large touch targets, accessible design |

**Free version:** unlimited reading, importing and sharing + 3 scans per day.
**ScanPilot Pro:** unlimited scans, merge, split, AI summaries, password lock, no watermark. Pro is sold through Google Play Billing (see [SALES_PLAN_EN.md](SALES_PLAN_EN.md)).

## How to Scan a Document to PDF on Android

1. Open ScanPilot and tap **Scan** (the big centre button).
2. Point your camera at the document — the guide frame helps you line it up.
3. Take the photo, choose colour, black-and-white or grayscale, and rotate or reorder pages if needed.
4. Tap save — your document is now a clean PDF in your library, ready to share.

No account. No internet. No watermark on the free plan's first scans.

## Screenshots

Screenshots of the real app are coming with the first Google Play release. The planned shots (see [store-listing-en.md](store-listing-en.md)):

1. **Home** — search bar, "Continue reading" and the tools grid
2. **Scan** — camera preview with guide frame and capture button
3. **Document view** — PDF page with night mode and page slider
4. **AI summary** — lecture notes summarized in seconds
5. **Library** — folders, contracts and favorites, always organized

## Who Is It For?

- **Students** — scan lecture notes, articles and textbooks; summarize them with AI.
- **Professionals** — scan contracts, invoices and reports; sign PDFs on the go.
- **Small businesses** — digitize receipts, quotes and paperwork; keep everything in folders.
- **Everyone** — scan IDs, certificates and personal documents and keep them password-protected.

## ScanPilot vs Other PDF Scanner Apps

| | ScanPilot | Adobe Scan | CamScanner | Genius Scan |
|---|---|---|---|---|
| Open source | ✅ MIT | ❌ | ❌ | ❌ |
| No account required | ✅ | ❌ | ❌ | ✅ |
| Ads | ❌ None | In-app purchases | ✅ Ads | Paid upgrade |
| Works offline | ✅ | Partly | Partly | ✅ |
| Merge & split PDF | ✅ | ✅ | ✅ | ✅ |
| AI summary | ✅ (your key) | ✅ | ✅ | ❌ |
| Password-protect PDF | ✅ | ✅ | ✅ | ❌ |

*Comparison based on the apps' public listings in November 2026. Prices and features change — check each app's Play Store page.*

## Getting Started — Build the App

**Requirements:** Android Studio (latest), Android SDK, internet for the first Gradle sync.

```bash
git clone https://github.com/kml11kml/Scanpilot.git
cd Scanpilot
```

1. Open the project folder in Android Studio (**File → Open**).
2. Let Gradle sync (first time needs internet to download dependencies).
3. Connect a phone with USB Debugging, or create an emulator.
4. For a quick test APK: `gradlew assembleDebug`
5. For Google Play, build a release App Bundle: **Build → Build App Bundle(s) / APK(s) → Build App Bundle(s)**
   - Output: `app/build/outputs/bundle/release/app-release.aab`
   - Sign it with your own release key — keep the key safe.

**Project facts:** package `com.scanpilot.app`, version 1.0.0, minSdk 24, targetSdk 36 / compileSdk 36 (Android 16, API 36), Kotlin + Jetpack Compose + Material 3, LTR English (US spelling).

> **Honest build note:** no AAB/APK was built or tested in the development VM (no Java/Android SDK there), and Google Play Billing in the code is a stub — wiring it up requires adding the Play Billing Library. See [BUILD_NOTES_EN.md](BUILD_NOTES_EN.md) for the full status.

**AI summaries (optional):** Settings → AI summary → enter your own API key (never in the code). Default endpoint `https://coarena.ai/api/v1`, default model `claude-fable-5-2`; any OpenAI-compatible service works.

## Tech Stack

- **Language:** Kotlin 1.9.24
- **UI:** Jetpack Compose, Material 3
- **Camera:** CameraX 1.3.4
- **PDF:** PdfRenderer (viewing), PdfDocument (creating), PDFBox-Android 2.0.27.0 (merge/split/lock)
- **Storage:** Room 2.6.1, DataStore 1.1.1
- **Network:** OkHttp 4.12.0
- **Build:** Android Gradle Plugin 8.5.2, Gradle 8.7

## FAQ — PDF Scanner Questions, Answered

**Is ScanPilot free?**
Yes. Reading, importing and sharing are unlimited and free, with 3 scans per day. ScanPilot Pro (unlimited scans, merge, split, AI summaries, password lock) is an optional in-app purchase on Google Play.

**How do I scan a document to PDF on Android?**
Open ScanPilot, tap Scan, photograph the document, and save — you get a clean PDF in seconds, with no account and no internet needed.

**Can I merge PDF files on Android?**
Yes. Select several PDFs in the library, tap Merge, and ScanPilot combines them into one file. Splitting works the same way.

**Can I sign a PDF on my phone?**
Yes. Open any PDF in the viewer, switch to edit mode, and draw your signature on the page.

**Does ScanPilot upload my documents?**
No. Files stay on your phone. Only the text of a document is sent to an AI service when *you* tap AI Summary — and only to the service whose key you entered yourself.

**Does it work offline?**
Yes — scanning, reading, annotating, merging, splitting and converting all work offline. Only AI summaries need the internet.

**Is there OCR?**
Not yet. OCR for scanned documents (extracting text from photos) is on the roadmap — see below.

**Where can I download ScanPilot?**
The Google Play release is in preparation (closed testing first, as required for new personal developer accounts). Until then, build it from source with Android Studio — see [Getting Started](#getting-started--build-the-app).

## Roadmap

- [ ] Google Play release (closed testing → production)
- [ ] OCR for scanned documents
- [ ] Word / Excel / PowerPoint import
- [ ] Real Google Play Billing wiring (Pro purchases)
- [ ] Scan cropping (edge detection)
- [ ] Persian OCR and RTL version (the original app, PdfYar, is Persian-first)

## Contributing

Contributions are welcome! ScanPilot is MIT-licensed open source.

1. Fork the repo, create a branch (`feature/your-idea`).
2. Keep the UI English (LTR) and the code clean and commented.
3. Test your change in Android Studio before opening a pull request.
4. Open a PR with a short description of what changed and why.

See [CONTRIBUTING.md](CONTRIBUTING.md) for details. Found a bug? Open an [issue](https://github.com/kml11kml/Scanpilot/issues).

## Privacy

Files stay on your device. No tracking, no ads in v1. AI summaries only send text when you tap the button. Full policy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## License

MIT License — see [LICENSE](LICENSE). Free to use, modify and share, with attribution.

---

**ScanPilot — Scan. Read. Summarize.**

*PDF scanner · scan to PDF · PDF reader · PDF editor · document scanner · merge PDF · split PDF · sign PDF · PDF converter · image to PDF · PDF to image · AI summary · free PDF app for Android · open source Android app · Kotlin · Jetpack Compose*
