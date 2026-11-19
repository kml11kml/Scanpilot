# Build Notes — ScanPilot version 1.0.0

## ScanPilot redesign — 2026-11-19 (US/UK Google Play version)

ScanPilot is a NEW product for the US and UK Google Play market, rebuilt from the
Persian app PdfYar (`~/workspace/pdfyar/` — untouched). Tagline: "Scan. Read. Summarize."

- App name: ScanPilot everywhere (was PdfYar). Package renamed `ir.pdfyar.app` →
  `com.scanpilot.app` (directories, package statements, namespace, applicationId,
  Room database file `pdfyar.db` → `scanpilot.db`).
- Google Play, not Cafe Bazaar: targetSdk 36, compileSdk 36 (Android 16, API 36);
  minSdk still 24. AGP is still 8.5.2 as inherited — Android Studio may require a newer
  Android Gradle Plugin (8.10+) to build against API 36; NOT verified here.
- Poolakey/Cafe Bazaar billing stub replaced with a Google Play Billing stub
  (`billing/BillingManager.kt`, class `PlayBillingManager`, honest TODO, no prices
  in code). Pricing suggestions ONLY in `SALES_PLAN_EN.md`, in USD: $4.99/month,
  $29.99/year (best value), $59.99 lifetime — set in Play Console, never in code.
- UI: LTR English (US spelling), Gregorian dates in locale format, Western digits.
  `toPersianDigits` was renamed `toWesternDigits` (same signature, returns Western digits).
  Rating links now open Google Play (`market://details?id=...` with a web fallback).
  Share goes through the Android share sheet (WhatsApp, Gmail, Drive); no Eitaa.
- Name: "ScanPilot" was chosen because the top US competitors are Adobe Scan, Genius Scan,
  SwiftScan and CamScanner — it does not collide with those names. **Trademark and
  Google Play availability were NOT verified — the developer must search Google Play
  for "ScanPilot" before finalising the name.**

## Google Play facts — NOT done (do not claim them)

- No AAB/APK was built or tested here (no Java/Android SDK in this environment).
- No Google Play Console account, listing, or submission exists.
- Closed testing: new personal Play Console accounts must run a closed test for at
  least 14 days before they can apply for production access.
- The Data Safety form in Play Console has not been completed (see PRIVACY_POLICY.md).
- The $25 one-time Play Console developer fee has not been paid.
- Google Play Billing is a stub only; the Play Billing Library has not been added
  and no in-app product exists in Play Console.

## English version — translated 2026-11-19 from the Persian original in ~/workspace/pdfyar/; no APK was built or verified here (no Java/Android SDK); features unchanged; Google Play Billing is a stub, not wired

Date: 2026-11-19 — built with Muse (main model) + limited help from Fable 5.2 (below).

## Verification status — honestly

- **A full APK build was not tested here.** There was no Java or Android SDK in this environment;
  only the project structure and the consistency of the code logic were checked. The user builds the APK in Android Studio on Windows.
- **No Google Play listing has been submitted** — the file `store-listing-en.md` is ready to copy into Play Console.
- **Real payment is not connected** — Google Play Billing is a stub (below).
- Camera (CameraX), PDF rendering and annotation have not been tested on a real device;
  their logic is written according to the Android documentation and must be tested on a phone.

## Help from Fable 5.2 (CoArena)

Three calls were made to `claude-fable-5-2` through
`~/workspace/custom-connectors/custom.coarena/bin/coarena.py`, and all three
failed with **HTTP 402 Payment Required** (reviewing PdfUtils, a small test, the CameraX scanner skeleton).
**No output from Fable was used; the subagent wrote all the code itself.**
No further attempts were made, so as not to use up the user's $5 daily budget.

## Files created

### Gradle and configuration
- `settings.gradle.kts` — app module
- `build.gradle.kts` (root) — AGP 8.5.2, Kotlin 1.9.24, KSP plugins
- `app/build.gradle.kts` — minSdk 24, targetSdk 36, compileSdk 36; namespace and applicationId `com.scanpilot.app`;
  Room 2.6.1, DataStore 1.1.1, OkHttp 4.12.0, PDFBox-Android 2.0.27.0, CameraX 1.3.4
- `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.7; the wrapper jar file is not included with the project — Android Studio creates it itself)
- `app/proguard-rules.pro`
- `.gitignore` (standard Gradle/Android project)

### Manifest and resources
- `app/src/main/AndroidManifest.xml` — CAMERA, READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE (maxSdk 32) permissions, FileProvider, PDF-view intent, home-screen scan shortcut
- `res/values/strings.xml`, `themes.xml`, `colors.xml`
- `res/xml/file_paths.xml`, `res/xml/shortcuts.xml` (the "Scan" shortcut with the `open_scan` extra)
- `res/drawable/ic_pdf.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` (main icon, not copied)

### Data
- `data/Entities.kt` — DocumentEntity, FolderEntity
- `data/Daos.kt` — including `observeBySize`
- `data/AppDatabase.kt` — Room
- `data/SettingsStore.kt` — AiSettings, onboardingSeen, successfulUses, ratePromptShown, scansToday/lastScanDate, isPro

### Utilities
- `util/PdfUtils.kt` — render/thumbnails with PdfRenderer, build PDFs with PdfDocument,
  merge/split by rendering pages (the text layer is not preserved — documented), PNG image output,
  `lockPdf` with PDFBox, `drawStrokesOnBitmap`, `extractText` (max 12,000 characters / 20 pages),
  `loadBitmapScaled`, `rotateBitmap`, `applyFilter` (colour/greyscale/black-and-white)
- `util/FormatUtils.kt` — Gregorian dates (SimpleDateFormat, Locale.US), `toWesternDigits` (renamed from `toPersianDigits`, returns Western digits), `formatSize`, `formatDate`, `sharePdf`
- `util/Annotations.kt` — HIGHLIGHT/UNDERLINE/STRIKE/DRAW/SIGN tools and AnnotationStroke with normalized 0–1 points

### Network, repository and billing
- `network/AiSummaryService.kt` — OkHttp POST to `{baseUrl}/chat/completions`, OpenAI-compatible; no hardcoded key; gives a clear error without a key
- `repository/DocumentRepository.kt` — SortOrder DATE/NAME/SIZE; import by copying to private storage; `createPdfFromPhotos` with watermark; merge, split, image output, lock, `saveAnnotatedPage` (max 50 pages); delete/rename/favourite/move; folders; 200 MB import cap
- `billing/BillingManager.kt` — interface + `LocalBillingManager` (stub; `purchasePro()` always fails with the honest message "not connected yet") + `PlayBillingManager` (empty Google Play Billing stub with a TODO). **There are no prices in the code.**

### ViewModels
- `viewmodel/LibraryViewModel.kt` — search/sort/folders/favourites, multi-select for merging, `showRatePrompt` after 3 successful uses and only once, `showProUpsell`, `isGridView`, split/lock/image output; merging requires Pro
- `viewmodel/ViewerViewModel.kt` — page navigation/rendering, favourites, summary (requires Pro + key), `saveAnnotations`
- `viewmodel/ScanViewModel.kt` — `FREE_SCANS_PER_DAY = 3`, watermark if not Pro, `saveAsPdf(name, filter, onSaved)`, records scans + successful uses, `showProUpsell` when the cap is reached
- `viewmodel/SettingsViewModel.kt` — settings, isPro, `purchasePro()`

### Theme and MainActivity
- `ui/theme/Color.kt` — primary colour dark teal 0xFF00695C, one primary colour everywhere
- `ui/theme/Theme.kt` — system font; no custom font is bundled
- `MainActivity.kt` — bottom navigation Home/Library/Scan (large middle button)/Tools/Settings, onboarding, `open_scan` shortcut handling, viewer route `viewer/{docId}?edit={edit}`

### Screens
- `ui/screens/HomeScreen.kt` — search, "Continue reading" (5 recent documents), unsaved-scans reminder card, Pro banner, 2-column tools grid with 10 tools
- `ui/screens/ToolsScreen.kt` — tools grid with descriptions + split/lock/image-output dialogs with document selection
- `ui/screens/LibraryScreen.kt` — search, sort chips (date/name/size), folder chips, thumbnails, rename/move/delete dialogs, rating prompt, Pro upgrade dialog, list/grid view, empty state with an "Add your first file" button, Western digits for page counts
- `ui/screens/ViewerScreen.kt` — pinch zoom, page slider + go to page, night mode with ColorMatrix, edit mode with annotation toolbar + Canvas, save annotations
- `ui/screens/ScanScreen.kt` — live CameraX preview with document guide frame, large capture button, colour/black-and-white/greyscale filters, retake = delete last photo, choose from gallery, camera permission handling, rotate/reorder, upgrade dialog when the cap is reached
- `ui/screens/SettingsScreen.kt` — AI settings, Pro section, support button
- `ui/screens/OnboardingScreen.kt` — 3 short pages, then start
- `ui/screens/SupportScreen.kt` — email placeholder `support@example.com` (the developer must change it); Google Play rating intent with a web fallback; privacy note

### Documentation
- `README.md` — building the AAB/APK in Android Studio on Windows + publishing on Google Play
- `PRIVACY_POLICY.md` — privacy policy draft for the Play Console Data Safety form
- `store-listing-en.md` — ready-made Google Play listing: title, short and full description, keywords, icon brief, 5 screenshots, 30-second video script
- `UI_UX_SPEC_EN.md` — UI specification for all screens, flows, colours, typography and the reference pattern (ScanPilot, LTR, Google Play)
- `SALES_PLAN_EN.md` — US/UK sales plan: customers, suggested USD prices (Play Console only), conversion and retention, ratings and trust, publishing and launch

## Simplifications and gaps (honestly)

- Merge/split is done by rendering pages; the text layer (searchable/copyable) is not preserved.
- `saveAnnotatedPage` saves at most 50 pages.
- `extractText` reads at most 12,000 characters from the first 20 pages.
- Importing a PDF larger than 200 MB is rejected.
- Document cropping is not implemented in the scanner; only rotate and reorder are available.
- There is no OCR — a scanned document has no text and the summary gives a clear message for it.
- No opening Word, Excel or PowerPoint (needs a dedicated SDK).
- No custom font is bundled — the system font is used.
- The Gradle wrapper jar file is not included with the project; Android Studio creates it.
- In LibraryScreen, the folder-delete dialog (`deleteFolderTarget`) is built but there is no button to open it — it should either be wired up or removed (note for the next version).
- In MainActivity there is an unused `BOTTOM_TABS` list and the middle bottom-navigation slot is disabled (the scan button sits separately above the navigation) — cleanup for the next version.
- The "Nothing found" message for a search with no results has not been added yet (an empty list is shown).
- Google Play Billing payment is a stub; prices are only suggested in `SALES_PLAN_EN.md`, in USD, not in the code.
- The support email is a placeholder: `support@example.com` — the developer must change it.

## APK build attempt on the dev VM — 2026-11-19 (failed, VM limitation)

- OpenJDK 17 (Temurin) and Android SDK (cmdline-tools, platforms;android-36, build-tools;36.0.0, platform-tools) were installed in this VM at `~/workspace/tools/` and `~/workspace/android-sdk/`.
- `gradle assembleDebug` fails at plugin resolution: `com.android.application:8.5.2` cannot be resolved. Root cause, proven with a Java test program and Maven: Java's HTTP/SSL client cannot download through this VM's egress proxy (read timeout / "Unsupported or unrecognized SSL message"), while curl downloads the same URLs fine. Gradle and Maven both use Java HTTP, so no dependency can be fetched here.
- No APK was produced in this VM. Build the APK on Windows in Android Studio (`gradlew assembleDebug`) or via the GitHub Actions workflow in `.github/workflows/build-apk.yml` (GitHub runners have normal internet access).
