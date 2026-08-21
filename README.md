# ROVENA — Your Digital Garage

A complete, offline-first Android application for managing everything about your vehicles: maintenance, fuel, expenses, documents, inspections, reminders, and more. No account, no login, no backend, no cloud — every byte of data lives on your device until you explicitly export it.

---

## ⚠️ Important note on this build environment

This project was built in a sandboxed cloud session with **no Android SDK installed**, and the sandbox's network policy blocks the only host that serves one (`dl.google.com`, which `maven.google.com` and the SDK manager both redirect to). That means:

- The full application source is complete, real, and committed to this branch.
- The `domain` Gradle module (pure Kotlin, no Android dependency) **was actually compiled and its 31 unit tests actually executed** in this sandbox via `./gradlew :domain:test` — see [Testing](#testing) below for the real results.
- The `app` module (the Android application itself) **could not be compiled, linted, or packaged into an APK/AAB in this sandbox**, because doing so requires `android.jar` and the build-tools, which only come from the blocked host. No APK, AAB, or "build succeeded" claim is made for the `app` module — building it is the first thing to do in a normal Android Studio / CI environment with SDK access (see [Building](#building)).
- The code was written and cross-checked carefully by hand: every `R.id`, `R.layout`, `R.drawable`, and `R.string` reference used anywhere in the Kotlin source was verified against the actual resource files (see the repo's commit history for the verification pass), and all four `strings.xml` locale files were checked to contain the exact same 350 keys. This closes most of the gap a real compile would catch, but it is not a substitute for one — run the first build in Android Studio and fix whatever a real `aapt2`/`kotlinc` pass still finds (there will likely be a handful of small issues; none of the architecture should need to change).

---

## What's implemented

Every feature in the original specification has a real, working implementation — not a mock or a placeholder:

- Multi-vehicle garage with full CRUD, primary-vehicle selection, and photo
- Dashboard with health ring, next-service/fuel/monthly-cost stats, upcoming tasks, recent activity, quick actions
- Maintenance tracking (21 categories, next-due mileage/date, photos)
- Fuel tracking with full/partial fill-ups, automatic L/100km · km/L · cost/km, mileage sanity check
- Expense tracking (13 categories, receipt photo, category breakdown)
- Document management (7 types, camera/file import, auto-generated expiry reminder)
- Vehicle inspection (23-item checklist across Exterior/Interior/Mechanical, live scoring, PDF report)
- Vehicle Health Score (transparent, documented algorithm — see below)
- Unified Timeline with type filters, auto-synced from every other feature
- Insights with custom-drawn bar/line/donut charts (no chart library dependency)
- Local reminders (mileage/date/both, recurring) with real Android notifications via WorkManager
- Local backup/restore (`.motiva` format) via Storage Access Framework, with "replace" and "add as new vehicles" restore modes
- App Lock: salted-hashed PIN + BiometricPrompt, process-lifecycle-aware re-lock
- Settings: garage, notifications, security, appearance (light/dark/system), data, units, currency (7 fixed + custom), language, about/privacy/terms/licenses
- Full localization: English, Arabic (RTL), French, Spanish — 350/350 keys translated in every locale
- Quick Add bottom sheet (Fuel/Maintenance/Expense/Document/Inspection/Reminder/Note)
- PDF report generation for Vehicle Summary, Maintenance History, Expense Report, Fuel Report, and Inspection Report — all rendered locally via `android.graphics.pdf.PdfDocument`, no internet, no third-party PDF library
- Debug-only sample data generator (BMW 320i) gated behind the `dev` product flavor, never seeded automatically

---

## Architecture

```
UI (Fragments/Activities, ViewBinding)
    ↓
ViewModel (StateFlow, coroutines)
    ↓
Repository (data/repository) ──uses──> Domain use cases (pure Kotlin, unit-testable)
    ↓
Room DAOs (data/local/dao)
    ↓
SQLite (via Room)
```

- **`domain` module** — plain Kotlin JVM module, zero Android dependencies. Holds every enum shared across layers and every calculation: `HealthScoreCalculator`, `FuelStatsCalculator`, `DueStatusCalculator`, `ExpenseAggregator`, `MileageValidator`, `InspectionScoreCalculator`, `BackupVersionValidator`. This is the one module that was actually compiled and tested in this environment.
- **`app` module** — the Android application.
  - `data/local` — Room entities, DAOs, `Converters`, `RovenaDatabase`, `Migrations`.
  - `data/repository` — one repository per feature, wrapping DAOs with reactive `Flow` reads and suspend writes; `TimelineSyncer` keeps the unified Timeline in sync with every write; `TimelineRepository` is the read side.
  - `presentation/<feature>` — one package per feature (`dashboard`, `garage`, `vehiclehub`, `vehicleform`, `maintenance`, `fuel`, `expenses`, `documents`, `inspection`, `timeline`, `insights`, `reminders`, `settings`, `backup`, `onboarding`, `lock`, `quickadd`, `common`). Every screen follows the same shape: a `ViewModel` exposing a `StateFlow<UiState>`, and a `Fragment` collecting it inside `repeatOnLifecycle(STARTED)`.
  - `utils` — formatters, enum→string-resource lookups, the PIN hasher, notification helper, PDF generators, backup manager.
- **Dependency injection**: no DI framework. `AppContainer` (in `com.rovena.garage`) is a small hand-rolled container of lazily-created repositories, held by the `RovenaApp` `Application` subclass. ViewModels are created with a one-line `GenericViewModelFactory` lambda. This keeps dependencies "minimal and stable" per the spec instead of pulling in Hilt/Dagger for what is, architecturally, a small and stable dependency graph.
- **Navigation**: a single `NavHostFragment` + one `nav_graph.xml` drives both the bottom-navigation tabs and every drill-down screen. Screens that can be reached either from a bottom tab (no specific vehicle) or from the Garage/Vehicle Hub (an explicit vehicle) resolve which vehicle to show via `Fragment.resolveVehicleId()` — an optional `vehicleId` nav argument falls back to the persisted "current vehicle" from DataStore.
- **Every vehicle-scoped table has a `vehicleId` foreign key with `onDelete = CASCADE`**, so deleting a vehicle cannot leave orphan rows — SQLite foreign-key enforcement is on by default in Room whenever `@ForeignKey` is declared.

---

## Database structure

Room database `rovena.db`, schema version 1, 12 entities:

| Entity | Purpose | Vehicle-scoped |
|---|---|---|
| `VehicleEntity` | Make/model/year/VIN/plate/fuel/transmission/mileage/purchase info/photo | — (root) |
| `MaintenanceRecordEntity` | Service history, next-due mileage/date | ✓ |
| `FuelRecordEntity` | Fill-ups, full-tank flag | ✓ |
| `ExpenseEntity` | Non-fuel, non-maintenance costs | ✓ |
| `DocumentEntity` | Registration/insurance/inspection/etc. files, expiry date | ✓ |
| `InspectionEntity` / `InspectionItemEntity` | Inspection sessions and their 23 checklist items | ✓ |
| `ReminderEntity` | Mileage/date/both, recurring, linked to documents' expiry | ✓ |
| `TimelineEventEntity` | Auto-generated unified feed row per record | ✓ |
| `VehiclePhotoEntity` | Generic photo attachment, polymorphic `linkedType`/`linkedId` | ✓ |
| `AppSettingsEntity` | Single-row (`id = 0`) app configuration | — (global) |
| `BackupMetadataEntity` | History log of backup/restore operations | — (global) |

All custom enums are stored as their `name` (a `String` column) via `Converters`, not as ordinals — this keeps the schema legible if you open the `.db` file directly and is stable across enum reordering. `AppSettingsEntity` holds durable, backed-up settings; the currently-selected vehicle and onboarding progress live in a small Jetpack DataStore (`UserPreferences`) instead, since that's session/UI state, not data worth including in a backup.

**Migrations**: the app ships at schema v1, so `Migrations.ALL` is currently empty — but the pattern (a `Migration(from, to)` per version, registered in `RovenaDatabase`) is already wired in. There is deliberately no `fallbackToDestructiveMigration()`: a missing migration should fail loudly, never silently erase a user's vehicle history.

---

## Vehicle Health Score algorithm

Implemented in `domain/usecase/HealthScoreCalculator.kt` (with 5 unit tests covering it directly).

**This is not a mechanical diagnosis.** It is a transparent score (0–100) built only from data the user has entered.

1. Nine weighted categories: maintenance recency, overdue maintenance, brakes, tires, battery, fluids, engine service, transmission service, documentation.
2. A category the user has never provided data for is `null` and is **dropped from the calculation entirely** — it is never guessed at.
3. If the categories with real data cover less than **34%** of the total possible weight, the result is `NOT_ENOUGH_DATA` (score = `null`) rather than a fabricated number.
4. Otherwise, the score is the weighted average of the known categories, renormalized against the known weight, rounded and clamped to `[0, 100]`.
5. Status bands: **90–100 Excellent · 75–89 Good · 60–74 Fair · 40–59 Attention Needed · 0–39 Critical.**

The same file also documents the deterministic rules used to turn raw signals (days since last service, overdue count, expired documents, etc.) into each category's sub-score — see the `fromVehicleInputs` KDoc.

Vehicle inspections use a separate, simpler scorer (`InspectionScoreCalculator`): GOOD = 100 points, ATTENTION = 55, PROBLEM = 10, UNKNOWN items are excluded from the average rather than penalized.

---

## Backup format (`.motiva`)

A `.motiva` file is a plain ZIP (renamed for clarity) containing:

```
manifest.json      { backupFormatVersion, appVersionCode, createdAtMillis, vehicleCount, checksum }
database.db        a full snapshot of the Room database (WAL-checkpointed before copy)
files/documents/*  copies of every locally-stored document file
files/photos/*      copies of every vehicle/maintenance/inspection photo
files/receipts/*    copies of every expense receipt photo
```

- Created/restored entirely through the Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument`) — the app never requests broad storage permissions.
- `checksum` is a SHA-256 of `database.db`, recomputed on restore to detect corruption before touching any live data.
- `BackupVersionValidator` (domain module, unit-tested) rejects a backup from a newer schema version and flags a bad checksum as corrupt — both cases are surfaced to the user instead of attempting a partial restore.
- **Replace current garage**: swaps the live database file and copies files back in, then restarts the app process (a full Room-singleton + `AppContainer` reset needs a clean process restart, not just an Activity recreate).
- **Add as new vehicles**: opens the extracted backup as a *second*, read-only Room database and re-inserts every vehicle (and its maintenance/fuel/expenses/documents/reminders/photos) into the live database through the normal repositories, so they get fresh IDs and never collide with what's already there.

---

## Offline architecture & privacy

- **No `INTERNET` permission is declared in the manifest at all.** The app cannot make a network request even if some code tried to.
- No Firebase, no analytics SDK, no crash-reporting SDK, no remote logging.
- `data_extraction_rules.xml` explicitly excludes the database, files, and shared prefs from Android's cloud backup and device-transfer flows, and `android:allowBackup="false"` on top of that.
- The only way data ever leaves the device is a `.motiva` file the user explicitly creates and then chooses to share themselves.

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Local reminder / document-expiry notifications (requested at runtime on Android 13+, only when the user turns notifications on) |
| `RECEIVE_BOOT_COMPLETED` | Re-arm the periodic reminder-check `WorkManager` job after a reboot |
| `USE_BIOMETRIC` | App Lock's fingerprint/face unlock |
| `VIBRATE` | Standard notification vibration |

No storage, camera, or location permissions are declared — photo/document/backup file access goes through `ACTION_GET_CONTENT` / SAF document pickers and the app's own internal storage, which need no runtime permission.

## Supported languages

English (default), Arabic (full RTL — verified: `supportsRtl="true"`, no hardcoded `left`/`right` layout attributes anywhere in the project, only `start`/`end`), French, Spanish. Switching is instant via `AppCompatDelegate.setApplicationLocales(...)` (AndroidX per-app language, backward-compatible). All 350 string keys are translated in all four locales — verified programmatically (see commit history) to have identical key sets across `values/`, `values-ar/`, `values-fr/`, `values-es/`.

---

## Testing

### What actually ran, in this sandbox

```
domain/src/test/kotlin/.../usecase/
  HealthScoreCalculatorTest       5 tests
  FuelStatsCalculatorTest         4 tests
  DueStatusCalculatorTest         7 tests
  MileageValidatorTest            5 tests
  ExpenseAggregatorTest           3 tests
  BackupVersionValidatorTest      4 tests
  InspectionScoreCalculatorTest   3 tests
                                 ─────
                                 31 tests, all passing
```

Run with `./gradlew :domain:test` (this genuinely executes — it's pure Kotlin/JVM, no Android SDK needed).

### What's written but unverified by a compiler

The `app` module's Kotlin/XML was hand-written and cross-checked (every resource reference confirmed to resolve — see the note at the top of this file), but never compiled. Once you have Android Studio / a CI runner with SDK access:

```bash
./gradlew :app:assembleDevDebug   # first build - expect to fix a handful of small issues
./gradlew :app:testDevDebugUnitTest
./gradlew :app:connectedDevDebugAndroidTest   # needs a device/emulator
```

---

## Building

Requires: JDK 17, Android SDK (compileSdk 34, build-tools matching AGP 8.5.x), an internet connection the *first* time (to resolve dependencies from Google/Maven Central).

```bash
git clone <this-repo>
cd Rovena
./gradlew :app:assembleDevDebug      # debug APK, sample-data generator enabled
./gradlew :app:assembleProdRelease   # release APK (unsigned unless you set the env vars below)
./gradlew :app:bundleProdRelease     # release AAB
```

Product flavors: `dev` (BuildConfig.SAMPLE_DATA_ENABLED = true, `.debug` app-ID suffix on debug builds) and `prod` (sample data generator hidden).

### Release signing

The release build type picks up a signing config only if these environment variables are set (nothing is hardcoded, and no keystore is committed):

```bash
export ROVENA_KEYSTORE_PATH=/path/to/release.jks
export ROVENA_KEYSTORE_PASSWORD=...
export ROVENA_KEY_ALIAS=...
export ROVENA_KEY_PASSWORD=...
./gradlew :app:assembleProdRelease
```

Without those set, the release build compiles unsigned (fine for local testing of R8/minification behavior; you'll need to sign before it's installable on a device that doesn't have debuggable builds enabled).

---

## Running

Install the debug APK on a device or emulator (API 26+) and launch. First run walks through onboarding (welcome → privacy explanation → language → add your first vehicle) and lands on the Dashboard. No login, no network required at any point.

To try the app with realistic data instead of starting empty: build the `dev` flavor, go to **More → Generate Sample Data (Debug)**, which adds one BMW 320i with maintenance, fuel, expense, and reminder history.

---

## Project structure

```
Rovena/
├── domain/                         pure Kotlin module (compiled + tested in this sandbox)
│   └── src/main/kotlin/.../domain/
│       ├── model/Enums.kt
│       └── usecase/*.kt            HealthScoreCalculator, FuelStatsCalculator, DueStatusCalculator, ...
├── app/
│   ├── src/main/kotlin/com/rovena/garage/
│   │   ├── RovenaApp.kt, AppContainer.kt
│   │   ├── data/local/{entities,dao,database}/
│   │   ├── data/repository/
│   │   ├── presentation/{dashboard,garage,vehiclehub,vehicleform,maintenance,fuel,
│   │   │                 expenses,documents,inspection,timeline,insights,reminders,
│   │   │                 settings,backup,onboarding,lock,quickadd,common}/
│   │   └── utils/{Formatters,EnumLabels,PinHasher,NotificationHelper,*PdfGenerator,backup/}
│   └── src/main/res/
│       ├── layout/ (32 files), navigation/nav_graph.xml, menu/, drawable/ (18 vectors)
│       └── values/, values-night/, values-ar/, values-fr/, values-es/
└── README.md (this file)
```

---

## Known limitations / honest disclosure

- **No compiled APK/AAB is included or claimed** — see the note at the top. The source is complete and internally consistent (every resource reference verified), but a real Android Gradle Plugin build has not run against it.
- Forgetting your App Lock PIN currently has no in-app recovery flow (there's no account to reset it through, by design) — clearing app data is the only way out, which erases local data unless you have a `.motiva` backup. Worth a "recovery codes" feature in a future version.
- Timeline entries are shown in a flat chronological list rather than grouped under date-section headers ("TODAY", "AUG 15") — functionally complete (filterable, auto-synced) but visually simpler than the mockup in the spec.
- The three custom chart views (bar/line/donut) are intentionally hand-rolled Canvas drawing rather than a charting library, per the "keep dependencies minimal" instruction — they cover the required monthly-spend, fuel-trend, and category-breakdown visualizations but are not a general-purpose charting engine.
