# Stock Manager — Android

Native Android version of the Stock Manager app, built with **Kotlin + Room + Material 3**,
for **Android 12 (API 31) and above**, with Material You dynamic color.

## Features

- 📊 **Dashboard** — total products, total stock, total stock value, low stock & out-of-stock counts, recent activity
- 📦 **Products** — name, category, **quality/grade** (A/B/C/D, editable), quantity, unit price, auto-calculated total, low-stock alert level — add/edit/delete/search
- ⬆⬇ **Stock** — one screen with an In/Out toggle, product picker, quantity, price, note, and a running log of recent moves
- 🕒 **History** — date range, type, and search filters over every stock movement
- 📑 **Reports** — Full Inventory / Low Stock / Out of Stock, each exportable
- 📄 **PDF export** (built with Android's own PDF engine — no extra libraries) and 📊 **CSV export** (opens perfectly in Excel, Google Sheets, WPS) everywhere, plus **CSV import** for bulk-adding/updating products
- 💾 **Autosave built in** — every change commits straight to the on-device database (Android's app storage is persistent by design, so there's no PyInstaller-style "temp folder" bug to worry about here). Settings also has one-tap **Backup Now** / **Restore Backup**, saved wherever you choose via the system file picker
- 🎨 **Material You** — the whole app tints itself to your wallpaper's colors on Android 12+, and follows your system light/dark setting automatically
- No storage permissions needed — file import/export uses the modern Storage Access Framework picker

## 1. Build the APK with GitHub Actions (no PC needed)

This project includes `.github/workflows/build.yml`, which builds the APK on GitHub's
own servers.

**One-time setup (from Termux):**
```bash
pkg install git gh -y
cd StockManagerAndroid
git init && git add . && git commit -m "Initial commit"
gh auth login
gh repo create StockManagerAndroid --public --source=. --remote=origin --push
```
That push kicks off the first build automatically.

**Getting the APK after a build finishes (~3–5 min):**
- Repo → **Actions** tab → open the run → **Artifacts** section → download the zip containing `app-debug.apk`
- Transfer/download it to your phone and tap to install (you may need to allow
  "install unknown apps" for your browser/file manager the first time — normal
  for any app installed outside the Play Store)

**For a permanent download link:**
```bash
git tag v1.0.0
git push origin v1.0.0
```
This publishes the APK on your repo's **Releases** page.

**Every time you want a new build:** just push a commit, or use **Actions → Build
Android APK → Run workflow** for a manual build with no code changes.

## 2. CSV import format

First row = headers (case-insensitive, any order). Recognized columns:

| Name | Category | Quality | Quantity | Price |
|------|----------|---------|----------|-------|

Only `Name` is required — everything else defaults sensibly if missing.

## 3. Notes on the build

- **minSdk 31 (Android 12)**, targetSdk/compileSdk 36, Kotlin 2.0.21, AGP 8.13.2
- Builds a **debug APK** (self-signed automatically, no signing setup needed) — perfect
  for installing on your own phone. If you ever want to publish to the Play Store,
  that needs a proper release signing key, which isn't set up here.
- If a build ever fails, open the failing step in the Actions log and paste the error
  back — that's usually enough to pinpoint a version mismatch or typo.

## Project structure

```
StockManagerAndroid/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/stockmanager/app/
│       │   ├── MainActivity.kt            toolbar + bottom nav + Navigation Component
│       │   ├── StockManagerApp.kt         applies Material You dynamic color
│       │   ├── data/                      Room entities, DAOs, database, prefs
│       │   ├── repository/                business logic + backup/restore
│       │   ├── export/                    PDF / CSV export + CSV import
│       │   └── ui/
│       │       ├── dashboard/
│       │       ├── products/
│       │       ├── stock/
│       │       ├── history/
│       │       ├── reports/
│       │       ├── settings/
│       │       └── common/                shared TransactionAdapter
│       └── res/                           layouts, Material3 themes, vector icons
├── .github/workflows/build.yml
└── build.gradle.kts / settings.gradle.kts
```
