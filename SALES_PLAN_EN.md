# Sales Plan — ScanPilot (US & UK, Google Play)

> A plan for selling well on Google Play in the US and UK.
> No sales figure is guaranteed; this plan is a route and a set of metrics, not a promise of income.

## 1. Who buys and why

| Customer | What they do with the app | The line they see in the listing and the app |
|---|---|---|
| Students | Lecture notes, articles, textbooks | "Scan your notes, get a summary" |
| Professionals | Contracts, invoices, reports | "Scan the contract, sign it, send it" |
| Small businesses | Receipts, quotes, paperwork | "Scan the receipt, keep it organized" |
| Everyone | IDs, certificates, documents | "Your important documents are always in your pocket" |

Rule: every page and every line of the listing should point to one of these four uses,
in simple language, without technical jargon.

## 2. Pricing (set in Play Console, not in the code)

The free version is genuinely useful every day:
unlimited reading, importing and sharing + 3 scans per day.
Pro version: unlimited scans, merge, split, AI summaries, password lock, no watermark.

**The purchase prompt appears only after the user has felt the value:**
- After the third successful scan in one day (when they hit the limit), or
- After they have tried merging once.
- Never on first launch, never with a forced pop-up.

**Suggested prices to test in Google Play Console (USD — suggestions only, not hardcoded in the code):**

| Plan | Suggested price | Note |
|---|---|---|
| Monthly | $4.99 | For starting out and testing |
| Yearly | $29.99 | **Best value** — highlight this one in the listing and the app |
| Lifetime | $59.99 | For loyal customers |

Test the prices for one month at these numbers first; if conversion is low, lower the monthly price,
not the yearly one. The final price is set only in Play Console; there is no price in the code.
Google Play takes a 15% service fee on the first $1M of annual earnings (30% above that,
under the current Play fee program — confirm the rate in Play Console before launch).

## 3. Store listing

The full listing is ready in `store-listing-en.md`: an exact title, a short description under 80 characters,
a full description with the keywords users actually search for
(PDF scanner, scan to PDF, PDF reader, PDF editor, merge PDF, AI summary), the Tools category,
an icon brief, 5 screenshots with captions and a 30-second video script.
Before publishing, take the screenshots with the real app interface; sample documents must not contain real personal information.

## 4. Conversion and retention inside the app (implemented)

- A 3-page onboarding that leads to scanning with one tap.
- "Continue reading" and recent documents on Home.
- Favorites, folders, search.
- Offline-first, fast start, dark mode.
- Home-screen shortcut "Scan document".
- A gentle reminder only when there is an unsaved scanned page (a card on Home, not a push notification).

## 5. Ratings and trust (implemented + developer checklist)

Implemented in the app:
- Rating prompt only after 3 successful uses and only once; never on first launch.
- Support page with an email placeholder (the developer must put in a real email).
- Privacy note: files stay on the phone and text is only sent when "Summary" is tapped.
- A corrupt or oversized file is rejected with a clear message, without crashing.

Developer checklist (only the developer can do these):
- Answer **all** Google Play reviews within 48 hours; especially answer 1- and 2-star reviews with an apology and a solution.
- Put a real support email on the support page and clear the inbox once a week.
- Rating goal: 4.5+ — if it drops below 4.2, fix the reported bugs first, then release a new version.

## 6. Publishing and launch (Google Play)

**Google Play first and only for this product** (US & UK). Package `com.scanpilot.app`,
targetSdk 36, compileSdk 36. AAB maximum 150 MB for the base module; our target is under 20 MB.

Launch checklist (only the developer can do these):
- [ ] Search Google Play for "ScanPilot" and check the trademark — the name was NOT verified.
- [ ] Create a Google Play Console developer account — one-time $25 fee.
- [ ] Run the required closed test (new personal accounts: at least 14 days of testing) before production.
- [ ] Complete the Data Safety form in Play Console (see PRIVACY_POLICY.md).
- [ ] In Play Console, create the ScanPilot Pro in-app products (monthly / yearly / lifetime) and set the prices from section 2.
- [ ] Add the Google Play Billing Library and complete the stub in `billing/BillingManager.kt` (`PlayBillingManager`).
- [ ] Put a real support email on the support page.
- [ ] Make the 5 screenshots, feature graphic and the 30-second video following `store-listing-en.md`.
- [ ] Create the app's social accounts; post the short video on YouTube Shorts, Instagram and TikTok.
- [ ] Give an update every 2 weeks with a changelog.

**Metrics to watch:**
Installs, free-to-Pro conversion, rating, crash rate.
If there are installs but no conversion → change the price or where the purchase prompt appears.
If the rating is low → fix the bugs first.
If crashes are high → release an urgent fix.

## 7. What is not in this version (honestly)

- OCR for scanned documents (summarizing scans with no text layer) — in the next version.
- Split and merge keeping the text layer — currently done by rendering pages.
- Opening Word, Excel and PowerPoint — needs a dedicated SDK.
- Real Google Play Billing payment — it is a stub; wiring it up is the developer's job.
- None of these is claimed in the listing.
