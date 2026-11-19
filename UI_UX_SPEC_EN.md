# UI/UX Specification — ScanPilot

> ScanPilot is the US/UK Google Play version of the app (package `com.scanpilot.app`).
> This spec describes a **left-to-right (LTR) English interface** (US spelling): all UI text in English,
> Western digits and Gregorian dates in locale format. Colours, layout structure, flows and behaviour are unchanged from the Persian original.

## Reference pattern and how we differ

The flow and layout pattern is taken from the app "PDF Editor: Scanner & Reader"
(a free app, 84.36 MB, rated 3.60 from 834 votes, version 1.0.0.8, with ads —
AppBrain review on 2026-11-19). Its icon, logo, screenshots and listing text were **not copied**;
only the familiar "home with tools + library + scan + viewer" pattern was rebuilt so users
already know the flow. We win on its weak points:

| Item | Reference app | ScanPilot (target) |
|---|---|---|
| Size | 84.36 MB | Under 20 MB |
| Ads | Yes | None |
| Spam notifications | Yes | None |
| Language | English | Fully English, left-to-right |
| Rating | 3.60 | Target 4.5+ with English support |

**Honest note:** the reference app also opens Word, Excel and PowerPoint files;
ScanPilot does not do that in version 1.0 (it needs a dedicated SDK) and
instead focuses on PDF, scanning, speed and a clean interface.

## General principles

- **Left-to-right on all screens** (LocalLayoutDirection = Ltr in MainActivity).
- **Fully English:** all interface text is in English; only the technical term PDF appears as-is.
- **Western digits and Gregorian dates** everywhere (library, viewer, scanner).
- **One primary colour:** dark teal `#00695C`, light `#B2DFDB`, dark `#004D40`;
  secondary blue-grey `#546E7A`; light background `#FAFAF7`, dark `#121212`.
- **Typography:** Android system font (no custom font is bundled). Body text size 16, titles 20, labels 14.
- **Dark mode:** follows the system theme (isSystemInDarkTheme) — a separate night-mode button in the viewer.
- **Large touch targets:** main button 56dp high, scan button 64dp, tool card 96dp high.
- **Fast start:** no sign-up, no account, no internet; a 3-page onboarding, then straight to the scan button.

## Screens and flows

### 1. Onboarding — 3 pages
- Page 1: "Scan your documents in seconds" (camera icon)
- Page 2: "All your documents, organized and offline" (folder icon)
- Page 3: "Smart summaries, whenever you want" (summary icon)
- "Next" / "Get started" button + "Skip"; shown only once.
- Flow: onboarding → Home.

### 2. Home
- Rounded search bar at the top of the screen (searches document names; "View all results" goes to the library).
- Gentle reminder card: if there is an unsaved scanned page, "You have unsaved scanned pages".
- "Continue reading": a horizontal row of the 5 most recent documents with first-page thumbnails.
- Free / Pro version banner (tap → Settings, purchase section).
- Tools grid, 2 columns, large cards with icon + label:
  Scan, Read PDF, Edit, Merge, Split, Image to PDF, PDF to Image, Lock with password, Sign, AI summary.
- Flow: tool → its screen; document → viewer.

### 3. Library
- Search, sort chips (Newest / Name / Size), favourites chip, "New folder +" chip.
- Folder filter row (All + user folders).
- Switch between list / grid view.
- Document row: first-page thumbnail, name, size, page count, Gregorian date, favourite star,
  "More" menu (share, rename, move to folder, delete).
- Long press: select multiple documents to merge (top bar shows "Merge" and "Cancel").
- Empty state: "You don't have any documents yet." + "Add your first file" button + "Scan with camera".
- Errors: a corrupt or oversized file is rejected with a clear English message (in the repository).

### 4. Viewer
- Full screen, pinch zoom (1x to 4x), page slider + "Go to page" + previous/next buttons.
- Top bar: back, night mode, edit, AI summary, favourite, share.
- Night mode: page colour inversion with ColorMatrix.
- Edit mode: the bottom toolbar is only visible in this mode —
  highlight, underline, strikethrough, drawing, signature + "Clear all" + "Save annotations".
  Saving creates a new "…-annotated" document; the original is untouched.
- AI summary: dialog with loading state / summary text / English error.
- Errors: corrupt or password-protected document → "This file cannot be opened." + explanation.

### 5. Scanner — CameraX
- Live camera preview with a white document-edge guide frame + the text "Place the document inside the frame".
- A large round capture button in the middle, gallery button and delete-last-photo button on either side.
- If camera permission is missing: a "Grant camera permission" button + choose from gallery.
- Filters: colour, black-and-white, greyscale (applied when saving).
- Photo list: thumbnails, move up/down, rotate 90 degrees, delete.
- Document name + large "Save as PDF" button (56dp).
- Free-version cap (3 scans per day) shown with an upgrade prompt, not a dry error.

### 6. Tools
- 2-column grid of all tools with one line of English description under each tool.
- Split / lock / PDF to image: first a document is selected, then the relevant dialog.
- Merge: guidance to the library (long press to select).

### 7. Settings
- AI summary section: API key (hidden), service address, model, on/off toggle + save.
- Pro version section: status + purchase button (currently an honest stub; see BillingManager).
- "Support and rating" button → support page.
- "About the app" section + privacy note.

### 8. Support
- Support email (currently the placeholder `support@example.com` — the user must change it).
- "Email support" and "Rate the app" buttons (opens the app's Google Play page).
- Privacy note in English.

## Bottom navigation

Home | Library | **Scan (large middle button)** | Tools | Settings.
The viewer, scanner and support screens have no bottom navigation (they have a back button).
The home-screen shortcut "Scan document" goes straight to the scanner.

## Empty and error states (checklist)

- Empty library → message + two action buttons.
- Home with no documents → "You don't have any documents yet".
- Search with no results → empty list ("Nothing found" message to be added in the next version — currently an empty list is shown).
- Corrupt / password-protected file → English message in the viewer.
- File larger than 200 MB → rejected with an English message when importing.
- No API key → "No API key entered" message in the summary.
- Scanned PDF without text → "This document has no text to summarize" message.
- No internet during a summary → "Connection failed".
