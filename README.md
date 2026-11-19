# ScanPilot — PDF Scanner & Reader

**Scan. Read. Summarize.**

ScanPilot is an offline, ad-free Android app for the US and UK Google Play market:
scan documents with your camera, read and organize PDFs, annotate and sign,
merge and split files, lock documents with a password, and get optional AI summaries.

- Package: `com.scanpilot.app` — version 1.0.0 (versionCode 1)
- Minimum Android: 24 — targetSdk 36, compileSdk 36 (Android 16, API 36)
- Language: Kotlin, Jetpack Compose, Material 3, left-to-right (LTR) English (US spelling)
- Name note: "ScanPilot" was chosen because the top US competitors are Adobe Scan,
  Genius Scan, SwiftScan and CamScanner — ScanPilot does not collide with those names.
  **Trademark and Google Play availability were NOT verified — search Google Play
  for "ScanPilot" before finalising the name.**

## Features in version 1.0

- Library: first-page thumbnails, search, sorting (date / name / size), list and grid views, folders, favorites, rename, move, delete
- Import PDFs from phone storage (max 200 MB) + a clear message for corrupt or oversized files
- Camera scanning (CameraX): document guide frame, color / black-and-white / grayscale filters, rotate and reorder pages, save as PDF
- Viewer: pinch zoom, page slider and go-to-page, night mode, annotation (highlight, underline, strikethrough, drawing, signature)
- Merge multiple PDFs and split one PDF (render-based — the text layer is not preserved)
- Convert images to PDF and PDF to images
- Lock a document with a password (PDFBox)
- AI document summaries — only when the user enters their own API key in Settings; without a key it shows a clear message
- Share via the Android share sheet (WhatsApp, Gmail, Drive, and more)
- Free + Pro: free = unlimited reading and sharing + 3 scans per day; Pro = unlimited scans, merge, split, AI summaries, locking, no watermark
- Gregorian dates in locale format, Western digits, dark mode, home-screen shortcut "Scan document"

## Building the AAB / APK on Windows (Android Studio)

1. Install Android Studio (latest version).
2. Open the `pdfyar-en` folder with Android Studio (File → Open).
3. Let Gradle sync; the first time needs internet (to download dependencies).
4. Connect a phone with a cable and turn on USB Debugging, or create an emulator.
5. For Google Play, build a release App Bundle: Build → **Build App Bundle(s) / APK(s) → Build App Bundle(s)**.
   For a quick test APK, use `gradlew assembleDebug` in the terminal.
6. The release bundle is built at:
   `app/build/outputs/bundle/release/app-release.aab`
7. Sign it with your own release key — keep the key safe; losing it means you cannot release the next version.

> In this environment (the virtual machine) there was no Java or Android SDK, so **no AAB/APK build was tested here** and should not be claimed.
> The developer builds and tests it in Android Studio on Windows.
> Note: the project targets API 36 with AGP 8.5.2 as inherited from the original project;
> Android Studio may require a newer Android Gradle Plugin (8.10+) to build against API 36 —
> this was not verified here, check the Gradle sync output first.

## Publishing on Google Play

1. Create a Google Play Console developer account — one-time fee $25.
2. New personal developer accounts must run a **closed test with at least 14 days of testing**
   before they can apply for production access — plan for this, it is not optional.
3. Complete the **Data Safety form** in Play Console (what data the app collects — see PRIVACY_POLICY.md).
4. Create a new app: package `com.scanpilot.app`, version 1.0.0, category "Tools" / Productivity.
5. Upload the signed release AAB, then copy the ready-made listing in `store-listing-en.md`
   (title, short and full description, screenshots, feature graphic).
6. Create the "ScanPilot Pro" in-app products in Play Console (monthly / yearly / lifetime);
   the suggested prices are in `SALES_PLAN_EN.md` — no prices are in the code.
7. Google Play in-app purchases use Google Play Billing; the code only contains an honest stub
   (`billing/BillingManager.kt`, class `PlayBillingManager`) and actually wiring it up
   requires adding the Play Billing Library — see the TODO in that file.

Important note: publish as **free with in-app purchase** from the start, not as a paid app;
changing from paid to free later is restricted on Google Play.

## Setting up AI summaries (optional)

1. In the app: Settings → AI summary.
2. Enter your own API key (the key is not in the code and must never be in the code).
3. The default address is `https://coarena.ai/api/v1` and the default model is `claude-fable-5-2`;
   you can enter the address and model of your own OpenAI-compatible service.
4. Without a key, the summary button shows the message "No API key entered".

## Privacy

Files stay on your phone. Only when you tap "AI summary" is the text of that document
sent to the service you chose yourself. The app has no tracking and no ads in v1.
Full policy: `PRIVACY_POLICY.md`.

## What is not in this version

- OCR for scanned documents (summarizing scans that have no text layer)
- Opening Word, Excel and PowerPoint files
- Real Google Play Billing payment (it is a stub — wiring it up is the developer's job)
- Full details in `BUILD_NOTES_EN.md`
