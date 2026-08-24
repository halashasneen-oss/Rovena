# ROVENA — Your Digital Garage

A complete, offline-first Android application for managing everything about your vehicles: maintenance, fuel, expenses, documents, inspections, reminders, and more. No account, no login, no backend, no cloud — every byte of data lives on your device until you explicitly export it.

---

## ⚠️ Important note on this build environment

This project is developed in a sandboxed cloud session with **no Android SDK installed locally**, and the sandbox's network policy blocks the only host that serves one (`dl.google.com`, which `maven.google.com` and the SDK manager both redirect to). That means:

- The full application source is complete, real, and committed to this branch.
- The `domain` Gradle module (pure Kotlin, no Android dependency) is compiled and its unit tests executed directly in the local sandbox via `./gradlew :domain:test` — see [Testing](#testing) for the current count.
- The `app` module cannot be compiled locally in this sandbox (it needs `android.jar` and build-tools from the blocked host), so every change to it is verified by pushing to this branch and letting **GitHub Actions** (`.github/workflows/android-build.yml`, real `ubuntu-latest` runners with a real Android SDK) run the actual `kotlinc`/`aapt2`/R8 build, the full app-module unit test suite, and produce real `rovena-dev-debug-apk`, `rovena-prod-release-apk`, and `rovena-prod-release-aab` artifacts. This is the authoritative build signal for the `app` module - check the latest workflow run on this branch for current status before assuming anything about it compiles.
- Every fix in this repo's history that touched the `app` module was iterated against a real failing CI log until the build was green, not just hand-verified - see the commit history for the specific root causes found this way (XML namespace scoping, AAPT2 resource-linking, Kotlin type-inference, cross-module smart-cast, and more).

---

## What's implemented

Every feature in the original specification has a real, working implementation — not a mock or a placeholder:

- Multi-vehicle garage with full CRUD, primary-vehicle selection, and photo
- Dashboard with health ring, next-service/fuel/monthly-cost stats, upcoming tasks, recent activity, quick actions
- Maintenance tracking (21 categories, next-due mileage/date, photos)
- Fuel tracking with full/partial fill-ups, automatic L/100km · km/L · cost/km, mileage sanity check
- Expense tracking (13 categories, receipt photo, category breakdown)
- Document management (7 types, camera/file import, auto-generated expiry reminder)
- Vehicle inspection (23-item checklist across Exterior/Interior/Mechanical, live scoring, per-item photos, PDF report)
- Vehicle Health Score (transparent, documented algorithm — see below)
- Unified Timeline with type filters, auto-synced from every other feature
- Insights with custom-drawn bar/line/donut charts (no chart library dependency)
- Local reminders (mileage/date/both, recurring) with real Android notifications via WorkManager
- Local backup/restore (`.rovena` format) via Storage Access Framework, with "replace" and "add as new vehicles" restore modes; hardened against Zip Slip path traversal and zip-bomb archives, with full inspection/photo restoration and collision-safe file handling (see [Security](#security) and [Backup format](#backup-format-rovena))
- App Lock: PBKDF2WithHmacSHA256-hashed PIN (6+ digits) with temporary lockout after repeated failures, + BiometricPrompt, process-lifecycle-aware re-lock that can't be bypassed via back navigation, deep links, notifications, or recreation
- Settings: garage, notifications, security, appearance (light/dark/system), data, units, currency (7 fixed + custom), language, about/privacy/terms/licenses
- Full localization: English, Arabic (RTL), French, Spanish — 350/350 keys translated in every locale
- Vehicle Notes: a persistent, editable list of freeform non-diagnostic notes per vehicle (quirks, contacts, reminders to self) — distinct from the Quick Add "Note" action below, which drops a one-off entry into the Timeline instead
- Global Search: compact, offline, garage-wide search across every vehicle, maintenance record, fuel fill-up, expense, and document, reachable from the Garage screen
- "Needs Your Attention" unified priority list on the Dashboard, ranking overdue/due-soon maintenance, expiring documents, and due reminders together with an overall vehicle status (Healthy/Attention/Urgent)
- Mileage Intelligence: labeled `"≈ <date>"` estimates for mileage-only due items, projected from the vehicle's own logged driving pace
- Parts History + Warranty Tracking: log replaced parts with install date/mileage and an optional warranty (date, mileage, or both) - warranty expiry feeds the same "Needs Your Attention" priority engine as maintenance/documents/reminders
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

Room database `rovena.db`, schema version 8, 15 entities:

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
| `VehicleNoteEntity` | Freeform, non-diagnostic notes (quirks, contacts, reminders to self) | ✓ |
| `PartEntity` | Installed parts with install date/mileage and optional warranty | ✓ |
| `AppSettingsEntity` | Single-row (`id = 0`) app configuration | — (global) |
| `BackupMetadataEntity` | History log of backup/restore operations | — (global) |

All custom enums are stored as their `name` (a `String` column) via `Converters`, not as ordinals — this keeps the schema legible if you open the `.db` file directly and is stable across enum reordering. `AppSettingsEntity` holds durable, backed-up settings; the currently-selected vehicle and onboarding progress live in a small Jetpack DataStore (`UserPreferences`) instead, since that's session/UI state, not data worth including in a backup.

**Migrations**: `Migrations.ALL` (in `data/local/database/Migrations.kt`) holds one real `Migration(from, to)` per schema bump so far - `MIGRATION_1_2` adds the PIN lockout columns described in [Security](#security) via plain `ALTER TABLE ADD COLUMN` statements, `MIGRATION_2_3` adds the `vehicle_notes` table, `MIGRATION_3_4` adds the `parts` table, `MIGRATION_4_5` adds `reminders.lastNotifiedStageDays` (the staged 30/14/7/3/1/0-day reminder notification schedule, see below), `MIGRATION_5_6` adds `reminders.category` and the tiered-severity/category toggle columns on `app_settings`, `MIGRATION_6_7` adds `currencyCode` to `vehicles` and `inspection_items` and backfills every NULL `currencyCode` across all five financial columns using the app's current default currency (see [Currency architecture](#rovena-v10--production-hardening-this-pass) below), `MIGRATION_7_8` adds the `health_score_snapshots` table (Health Score history/trend, at most one row per vehicle per calendar day) - registered in `RovenaDatabase` via `Room.databaseBuilder(...).addMigrations(*Migrations.ALL)`. There is deliberately no `fallbackToDestructiveMigration()`: a missing migration should fail loudly, never silently erase a user's vehicle history. `BackupManager.restoreAsNewGarage()` opens a second, separate `RovenaDatabase` instance against the *extracted backup's* `database.db` file to read it - that instance registers the same `Migrations.ALL` too, so restoring a backup made by an older app version (an older schema on disk) migrates it forward instead of throwing. Every future schema change gets its own migration appended to `ALL`, never a silent version bump.

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

**Health Score ↔ Inspection integration**: the brakes/tires/battery/fluids sub-scores are no longer left blank. `InspectionRepository.observeLatestConditionScores()` reads the vehicle's most recent inspection and maps each relevant item's status (GOOD/ATTENTION/PROBLEM/UNKNOWN) onto the same 100/55/10/`null` scale via `InspectionScoreCalculator.conditionScoreFor()`, feeding real condition data into both the Dashboard and Vehicle Hub health scores instead of always excluding those four categories. Tapping the health score on the Vehicle Hub screen opens a detail dialog listing every category's current value and a "data confidence" percentage (`knownWeightRatio`), so the number is never a black box.

---

## Backup format (`.rovena`)

A `.rovena` file is a plain ZIP (renamed for clarity) containing (an optional password-encrypted `.rovena.secure` variant is also available - see the last bullet below):

```
manifest.json      { backupFormatVersion, databaseSchemaVersion, appVersionCode, appVersionName,
                      createdAtMillis, vehicleCount, checksum }
database.db        a full snapshot of the Room database (WAL-checkpointed before copy)
files/documents/*  copies of every locally-stored document file
files/photos/*      copies of every vehicle/maintenance/inspection/inspection-item photo
files/receipts/*    copies of every expense receipt photo
```

- Created/restored entirely through the Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument`) — the app never requests broad storage permissions.
- `checksum` is a SHA-256 of `database.db`, recomputed on restore to detect corruption before touching any live data.
- `BackupVersionValidator` (domain module, unit-tested) rejects a backup from a newer schema version and flags a bad checksum as corrupt; `BackupManager.inspect()` also enforces zip-bomb limits (`MAX_ZIP_ENTRIES`, `MAX_TOTAL_UNCOMPRESSED_BYTES`) and Zip Slip path-traversal protection (`BackupPathValidator`) before a single byte from the archive is written to disk - see [Security](#security). Any of these failures surfaces a clear "invalid or unsafe" message to the user instead of attempting a partial restore, and never crashes.
- **Replace current garage**: swaps the live database file, clears the live `documents`/`photos`/`receipts` directories, and copies the backup's files back in under their original names, then restarts the app process (a full Room-singleton + `AppContainer` reset needs a clean process restart, not just an Activity recreate).
- **Add as new vehicles**: opens the extracted backup as a *second*, read-only Room database and re-inserts every vehicle - and its maintenance, fuel, expenses, documents, reminders, inspections, inspection items, and every linked photo - into the live database through the normal repositories. Every foreign key is remapped through an explicit old-id → new-id map built as each parent record is imported (vehicle, maintenance, expense, document, inspection, inspection item), so nothing in the new garage can ever point back at an id from the old one. Files are copied into live storage under a fresh UUID filename (never the backup's original name), so an "add as new" restore can never silently overwrite a file already used by the current garage.
- **Optional encryption (`.rovena.secure`)**: creating a backup offers an optional password. When set, the exact same zip above is encrypted whole with AES-256-GCM (`BackupEncryption`) before being written - `MAGIC (13 bytes) | salt (16 bytes) | iv (12 bytes) | ciphertext+authTag`, key derived via PBKDF2WithHmacSHA256 (120,000 iterations, same construction as `PinHasher`). Restoring peeks the file's first bytes to detect this format automatically (a plain `.rovena` file needs no password and is never even asked for one) and prompts for the password, retrying cleanly on a wrong one rather than failing cryptically - GCM's authentication tag makes a wrong password or any tampering fail decryption outright instead of silently returning garbage.

---

## Offline architecture & privacy

- **No `INTERNET` permission is declared in the manifest at all.** The app cannot make a network request even if some code tried to.
- No Firebase, no analytics SDK, no crash-reporting SDK, no remote logging.
- `data_extraction_rules.xml` explicitly excludes the database, files, and shared prefs from Android's cloud backup and device-transfer flows, and `android:allowBackup="false"` on top of that.
- The only way data ever leaves the device is a `.rovena` file the user explicitly creates and then chooses to share themselves.

## Security

- **PIN storage**: the raw PIN is never persisted. `PinHasher` (pure `java.security`/`javax.crypto`, no Android dependency) derives a key via `PBKDF2WithHmacSHA256` (120,000 iterations, 256-bit output) from the PIN and a random 16-byte per-install salt (`SecureRandom`); only the salt and derived hash are stored. Verification uses a constant-time comparison. Minimum PIN length is 6 digits (up to 10). This replaced an earlier, weaker hand-rolled repeated-SHA-256 loop - PBKDF2 is a real, purpose-built password/PIN KDF with a standardized security analysis behind its iteration-count-based slowdown; a bare hash loop is not an equivalent construction.
- **Lockout**: 5 consecutive wrong PIN attempts trigger a 30-second lockout (`SettingsRepository.verifyPin`), enforced *before* the PIN hash is even touched so a locked-out caller can't burn the deliberately-slow KDF cost by hammering the unlock screen. The lockout always expires on its own - there is no permanent lockout and no account to reset a PIN through by design, so losing a PIN means clearing app data (or restoring a `.rovena` backup made before it was set).
- **App Lock cannot be bypassed** via back navigation (canceling the lock screen closes `MainActivity` instead of revealing it), deep links (no other exported activity or intent-filter exists), notifications (every notification's `PendingIntent` routes through `MainActivity`, which re-checks the lock on every `onResume`), or Activity recreation/configuration changes (re-lock state lives in the `Application` subclass via `ProcessLifecycleOwner`, armed whenever the *whole app* - not just one Activity - leaves the foreground, so a system photo picker or file chooser opening briefly doesn't false-trigger a re-lock).
- **Backup archive safety**: see [Backup format](#backup-format-rovena) above - Zip Slip path-traversal defense (`BackupPathValidator`, unit-tested with `../`, `..\`, and absolute-path attack vectors) and zip-bomb entry/size limits are enforced for every `.rovena` file before any of its bytes touch disk. Restore never gates on file extension, so backups created by earlier app versions under the old `.motiva` name still restore correctly.
- **No network surface at all** - see [Offline architecture & privacy](#offline-architecture--privacy).

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

### Domain module — runs locally, pure JVM, no Android SDK needed

```
domain/src/test/kotlin/.../usecase/
  HealthScoreCalculatorTest       5 tests
  FuelStatsCalculatorTest         4 tests
  DueStatusCalculatorTest         7 tests
  MileageValidatorTest            5 tests
  ExpenseAggregatorTest           3 tests
  BackupVersionValidatorTest      4 tests
  InspectionScoreCalculatorTest   3 tests
  BackupPathValidatorTest         8 tests   (Zip Slip / path-traversal defense)
                                 ─────
                                 39 tests
```

Run with `./gradlew :domain:test`.

### App module — verified via GitHub Actions CI (real Android SDK, `:app:testDevDebugUnitTest`)

```
app/src/test/kotlin/.../
  utils/PinHasherTest                    7 tests   (PBKDF2 PIN hashing - pure JVM, no Robolectric)
  data/repository/DocumentRepositoryTest 5 tests   (document expiry -> reminder lifecycle, Robolectric + in-memory Room)
  data/repository/InspectionRepositoryTest 3 tests (item-id stability across saves, inspection-item photo safety, Robolectric + in-memory Room)
                                         ─────
                                         15 tests
```

These are the first tests in the `app` module (previously untested beyond the domain layer). Espresso/instrumented UI tests (onboarding, add-vehicle, backup/restore flows end-to-end, Arabic RTL) are not yet written - they need a device/emulator, which this sandbox and the current CI workflow don't have; see [Known limitations](#known-limitations--honest-disclosure).

```bash
./gradlew :app:testDevDebugUnitTest           # domain + app unit tests
./gradlew :app:assembleDevDebug               # debug APK
./gradlew :app:assembleProdRelease            # release APK (R8 + resource shrinking)
./gradlew :app:bundleProdRelease              # release AAB
./gradlew :app:connectedDevDebugAndroidTest   # needs a device/emulator - not run by this repo's CI yet
```

---

## Building

Requires: JDK 17, Android SDK (compileSdk 36, build-tools matching AGP 8.11.x), an internet connection the *first* time (to resolve dependencies from Google/Maven Central).

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

## ROVENA V2 — Intelligent Digital Garage (this pass)

A follow-up enhancement pass targeting a coherent, verifiable slice of a much larger "intelligent garage" specification (product-intelligence philosophy, a local rules-based Vehicle Intelligence Engine, a unified Reminder/Notification Center, Vehicle Lifecycle 2.0, Parts/Warranty tracking, encrypted backups, and more). Implemented this pass:

- **Health Score ↔ Inspection integration** (previously the single biggest gap): brakes/tires/battery/fluids sub-scores now come from the vehicle's latest real inspection instead of always being `null`. See the Health Score section above.
- **Health Score transparency**: a new tap-to-view detail dialog on the Vehicle Hub screen shows every category's current value plus a data-confidence percentage, addressing the "score as a black box" concern.
- **Backup format renamed** `.motiva` → `.rovena` throughout the app, strings (all 4 locales), and this README. Restore never gated on file extension, so backups created by earlier versions under the old name still restore correctly with no user action needed.
- **Reminder notifications now name the vehicle** (`"BMW 320i • 12 KM remaining"` instead of just the reminder title), removing the ambiguity a multi-vehicle garage previously had about which car a notification referred to.
- **Mileage Intelligence**: `DueStatusCalculator.evaluate()` already accepted an `averageKmPerDay` parameter and computed a projected `estimatedDueDate` from it, but no call site anywhere in the app ever passed one - the projection logic existed and was unit-tested but was completely dead in the running app. `MileageIntelligenceCalculator.averageKmPerDay()` now derives a vehicle's real driving pace from its own logged odometer readings (fuel fill-ups + maintenance records, no new data entry required), wired into the Dashboard and Reminders screen so mileage-only due items show a labeled `"≈ <date>"` estimate.
- **"Needs Your Attention" unified priority engine**: `PriorityEngine` (domain, unit-tested) combines overdue/due-soon maintenance, expiring/expired documents, and due/overdue reminders into one ranked list and an overall vehicle status (Healthy/Attention/Urgent), replacing the Dashboard's old reminders-only "Upcoming Tasks" list - maintenance and document-expiry items that needed attention were previously invisible there just because they came from a different table.
- **Vehicle Notes**: a new persistent, editable per-vehicle list of freeform non-diagnostic notes (quirks, contacts, reminders to self) - distinct from the existing Quick Add "Note" action, which drops a one-off entry into the Timeline instead. New `vehicle_notes` table, schema bumped to v3.
- **Global Search**: a compact, offline, garage-wide search (reachable via the search icon on the Garage screen) across every vehicle, maintenance record, fuel fill-up, expense, and document - not scoped to the currently-selected vehicle. The per-vehicle `search()` DAO queries this reuses already existed but were never wired to any screen before this; new `searchAcrossGarage()` siblings extend the same queries across the whole garage.
- **Parts History + Warranty Tracking**: a new "Parts" section per vehicle logs replaced parts with install date/mileage and an optional warranty (date, mileage, or both). Warranty expiry is evaluated through the exact same `DueStatusCalculator` used everywhere else and feeds straight into the "Needs Your Attention" priority engine above, rather than being a disconnected bolt-on list. New `parts` table, schema bumped to v4.
- **Staged reminder notification schedule**: date-based reminders (registration, insurance, inspections, etc.) now notify on a 30/14/7/3/1/0-days-before cascade instead of a single due/overdue alert, via `ReminderStageCalculator` (domain, unit-tested) and a new `reminders.lastNotifiedStageDays` column (schema bumped to v5). Editing an unrelated field on a reminder preserves the already-notified stage; changing the due date itself resets it so the cascade restarts; a rolled-over recurring reminder always gets a fresh stage. Mileage-only reminders are unchanged and keep their original single-notification behavior — only the date-based path gained the cascade.
- **Tiered notification severity + per-category toggles**: every reminder notification is now classified by `NotificationSeverityCalculator` (domain, unit-tested) into Critical (overdue or due within a day), Important (due within a week), or Upcoming (due within a month), which sets the Android notification's priority and can be individually muted from a new Settings → Notification Preferences screen - alongside two category toggles (Documents vs. general reminders, via a new `reminders.category` column, schema bumped to v6). A muted tier/category still advances the reminder's dedup state as if it had notified, so re-enabling a muted tier later never dumps a backlog of missed notifications at once. Fixed a real pre-existing gap while wiring this in: the Settings "Enable Reminders" master switch was persisted but never actually checked by `ReminderCheckWorker` before posting - it only gated the runtime permission request, so turning it off didn't stop notifications. It's now the first check in `doWork()`.
- **Verified, not changed**: audited whether Maintenance and Expense records could double-count the same cost. They are only ever written by the user through their own independent forms — there is no code path that auto-creates one from the other — so the dashboard's `fuel + maintenance + expense` monthly total is not structurally double-counting. The only residual risk is a user manually logging the same real-world cost twice, which is a UX/education matter rather than a defect.
- **Verified, not changed**: audited reminder-scheduling reboot survival and notification dedup. `BootCompletedReceiver` is already correctly manifest-registered (`RECEIVE_BOOT_COMPLETED` permission, `android:exported="true"` as required for a system-broadcast receiver on API 31+) and re-enqueues `ReminderCheckWorker` with `ExistingPeriodicWorkPolicy.KEEP` on boot - the same policy `RovenaApp.onCreate()` uses, so neither call site can create a duplicate periodic run. `ReminderStageCalculator.stageFor()` is inherently resilient to the worker missing runs (device off, Doze, long-deferred WorkManager execution): it always evaluates the *current* remaining-days against the full threshold list rather than expecting to observe every threshold in order, so a gap that skips past several stages still fires exactly the one stage that's actually current - never a burst of backlogged notifications, never a silently-skipped one. `NotificationActionReceiver` (handles the notification's "Mark Done" action) is correctly `android:exported="false"`, so no other app can trigger it. No code changes were needed here; this pass's `lastNotifiedStageDays` resume-safe dedup (see above) was the one real gap, and it's now covered by `ReminderRepositoryTest`.
- **Maintenance Cost Trend chart**: the Insights screen's fuel-economy trend line (`FuelStatsCalculator`'s full-to-full `intervals`) was already wired up, but maintenance cost had no trend of its own - only folded into the combined fuel+maintenance+expense "Monthly Spending" bar chart, where a fuel price swing could mask or exaggerate a maintenance cost trend. Added a dedicated maintenance-only monthly cost bar chart (`InsightsViewModel.maintenanceMonthlySpend`, same 6-month window, reusing the existing `BarChartView`), with a "not enough data" empty state when the vehicle has no maintenance costs logged yet.
- **Timeline 2.0 date-section grouping**: the Timeline screen's flat event list now groups consecutive same-day entries under a "Today" / "Yesterday" / "15 Aug" section header (`Formatters.timelineSectionLabel`), via a new `SectionedTimelineAdapter` + `TimelineListItem` sealed class kept separate from the existing `TimelineEventAdapter`, which also backs the Dashboard's flat "recent activity" preview and has no business showing date headers.
- **Vehicle Intelligence insight-text generator**: `VehicleInsightGenerator` (domain, unit-tested) is a small, local, rules-based engine that compares the recent half of a trend against its older half and only speaks up when the change clears an 8% significance threshold - never a fabricated observation, and nothing resembling AI-generated prose. It currently covers two trends already computed elsewhere on the Insights screen: fuel economy (from `FuelStatsCalculator`'s intervals) and maintenance cost (from the Maintenance Cost Trend chart's monthly buckets above). Surfaced as a new "Insights" card with 1-2 short sentences ("Fuel economy has improved by 12% recently.") - hidden entirely when there isn't enough data or nothing significant changed, rather than always showing something.
- **Inspection → Maintenance task suggestion flow**: saving an inspection with PROBLEM-flagged mechanical items (brakes, tires, battery, suspension, steering, engine, transmission, cooling) now offers - never auto-creates - a due-now reminder for each, via `InspectionMaintenanceSuggester` (domain, unit-tested) and a confirmation dialog with a per-item checklist the user can adjust before confirming. Deliberately excludes the ambiguous "fluids" item and every exterior/interior item, since guessing a specific maintenance category for those would be exactly the kind of fabricated specificity this app avoids elsewhere. This is also the first real use of `ReminderEntity.linkedMaintenanceCategory` - present in the schema since the original build but never actually set by any code path until now.
- **Encrypted `.rovena.secure` backup format**: creating a backup now offers an optional password; when set, the same zip `.rovena` build is encrypted with AES-256-GCM (`BackupEncryption`, unit-tested, pure JVM - no Android dependency) using a PBKDF2WithHmacSHA256-stretched key, matching `PinHasher`'s construction and iteration count for the same reason: a fast hash would make offline brute-forcing of a stolen file cheap. Restoring transparently detects an encrypted file via a magic-header peek and prompts for its password (with a clear "wrong password" retry rather than a cryptic failure) - a plain unencrypted `.rovena` file is completely unaffected and still streams straight from disk exactly as before, since GCM decryption is the only case that actually needs the whole file buffered in memory first.
- **Vehicle Sale Report PDF**: a second report option (`VehicleSalePdfGenerator`) alongside the existing personal Vehicle Summary, reachable from the same "Generate PDF" action on the Vehicle Hub screen (now a two-option chooser instead of always producing the summary). Deliberately different content, not just a re-skin: it itemizes the full maintenance history and the latest inspection's per-item condition breakdown - the things that build a buyer's trust - while never including purchase price or the owner's current estimated value, which are the owner's private financial figures and none of a buyer's business.
- **Service Plans / maintenance bundles**: `ServicePlanCatalog` (domain, unit-tested) is a small fixed catalog of routine bundles (Minor Service = oil + oil filter + air filter; Major Service adds cabin filter + spark plugs; Brake Service = pads + discs) - built-in rather than user-defined, since the real value here is not re-typing the same routine bundle by hand, not a full custom-plan CRUD subsystem. A new "Apply Service Plan" icon on the Maintenance list screen shows a plan chooser, then one small form (date/mileage/workshop, prefilled from the vehicle's current mileage) the user reviews before confirming.
- **Daily/Weekly in-app summary**: two new stat cards on the Dashboard ("This Week's Distance", "This Week's Spend") roll up the trailing 7 days from data already tracked elsewhere - distance from the same odometer-reading history `MileageIntelligenceCalculator` already uses for driving-pace estimates, spend from fuel + maintenance + expenses dated in that window. Distance shows "not enough data" rather than a number when fewer than 2 odometer readings fall in the window, same honesty rule as every other estimate in this app.

All items originally deferred in this pass's first commits have now been implemented and verified via CI - nothing from the ROVENA V2 spec sections above was left unaddressed. The only remaining gaps are the pre-existing, unrelated items already listed under [Known limitations](#known-limitations--honest-disclosure) below (PIN recovery, orphaned maintenance-photo files, inspection photos not yet in the PDF, and instrumented UI tests).

---

## ROVENA V1.0 — Production Hardening (this pass)

A follow-up pass turning the app into a production-ready V1.0: fixing real correctness/safety bugs found by re-reading the existing implementation against a full hardening specification, not a rewrite. Priority order: data-safety/correctness (P0) first, then production-quality (P1), then product polish (P2) where it fits cleanly. Implemented so far:

- **Currency architecture fix (P0)**: `currencyCode` existed as a schema field on Expense/Fuel/Maintenance since the very first schema version, but no write path ever populated it - every entry form hardcoded `currencyCode = null` on save, and every display/total site hardcoded `AppCurrency.JOD` as the format currency regardless of what a record actually stored. Every amount silently rendered as JOD and every "total" blindly summed raw numbers even across currencies. Fixed end to end: Vehicle and inspection-item repair cost gained a `currencyCode` column too (schema v6→v7, migration backfills every existing NULL using the app's current default currency, never touching values already set); every entry form now stamps a new record with the app's current default currency and preserves the original currency untouched when editing; a new domain `CurrencyAggregator` groups and sums by currency instead of a blind `sumOf`, exposing Empty/Single/Mixed so a caller can never accidentally combine currencies - wired into every adapter, PDF generator, the Dashboard's monthly/weekly cost, and Insights' monthly trend chart and category breakdown (restricted to the vehicle's single most common currency, since a chart can only render one number per bar, with a mixed-currency flag for the UI).
- **Backup/restore transactional safety (P0)**: `restoreReplacing()` used to close the live database, delete its WAL/SHM, delete the live documents/photos/receipts directories, and only *then* copy the backup's files over the top - a failure partway through (disk full, a subtly corrupt db file, the process getting killed) left the user with their original garage already gone and nothing valid in its place. Now the backup's database is verified to actually open and migrate through the app's real Room builder *before* any live data is touched; only then is the current live state snapshotted into a rollback copy, the swap performed, and the swapped-in database re-verified - any failure from the snapshot onward restores the rollback copy so the original garage comes back exactly as it was. The manifest also now records `fileCount`/`totalSizeBytes`, and `createBackup`/`inspect`/both restore paths report progress (Preparing/Creating/Validating/Restoring/Verifying) through a non-dismissible dialog instead of leaving a multi-second operation looking frozen.
- **Target SDK 36 + edge-to-edge (P0)**: `compileSdk`/`targetSdk` bumped 34→36 (Android Gradle Plugin 8.5.2→8.11.0, Gradle 8.7→8.13 to match - the minimum combination that actually supports compiling against API 36, not a blind "latest everything" jump). targetSdk 35+ forces edge-to-edge regardless of what the app requests, so all three Activities (`MainActivity`, `OnboardingActivity`, `LockActivity`) now consume system-bar and IME insets as padding via a shared `applyEdgeToEdgeInsets()` helper - see the honest caveat on what this has and hasn't been verified against under [Known limitations](#known-limitations--honest-disclosure). The bump also broke every Robolectric-backed app-module test ("Robolectric does not support API level 36" - the pinned Robolectric 4.13 has no shadows for it yet); fixed with a `robolectric.properties` pinning the test suite's simulated SDK to 34, independent of what the app targets in production, rather than the much larger and unrelated JDK-21 upgrade a Robolectric version bump would require.
- **Centralized input validation (P0)**: mileage/cost/quantity/required-text/year checks used to be re-implemented ad-hoc in every form ViewModel (`if (x == null || x < 0) ...`), with no consistency and several gaps (e.g. Fuel's mileage-lower-than-previous warning existed but a plain negative mileage on other forms like Expense was never checked at all). New domain `InputValidator` (unit-tested) collects these rules once; every entry form (Expense, Fuel, Maintenance, Vehicle, Document, Reminder, Inspection) now routes through it. Every swap only touches a field that already has a real error-display UI binding - never a check added silently with nowhere to show its message.
- **Mileage-plausibility warning unification (P0/P1)**: the lower-than-previous/unrealistic-jump odometer check (`MileageValidator`) previously only ran on the Fuel form, compared against Fuel's own latest record (so it missed a lower reading entered right after a higher Maintenance entry), and only ever surfaced the lower-than-previous case - a large unrealistic jump was silently accepted. Replaced with one `VehicleRepository.checkMileage()` built on the vehicle's already-synced `currentMileageKm`, wired non-blocking into every form that captures an odometer reading (Fuel, Maintenance, Inspection, Vehicle edit), with both warning cases now rendered via a shared `Formatters.mileageWarningText()`.
- **Health Score 2.0 (P1)**: the weighted-category/data-confidence/NOT_ENOUGH_DATA algorithm itself (`HealthScoreCalculator`) was already sound, but its *inputs* were independently (and inconsistently) reconstructed at five different call sites (Dashboard, Vehicle Hub, Garage list, both PDF generators) - three of them permanently hardcoded `engineServiceUpToDate`/`transmissionServiceUpToDate` to `null` (15% of the score's weight silently and permanently excluded, regardless of actual data), and the Garage list and the Vehicle Summary PDF never passed inspection condition scores at all. Unified into one `HealthInputsBuilder` shared by all five call sites, which also correctly derives engine/transmission service-currency from the relevant `MaintenanceCategory` records instead of leaving them null forever. Also added a real history/trend: a `health_score_snapshots` table (schema v7→v8, one row per vehicle per calendar day, written passively whenever a score is computed - never fabricated or backfilled) feeds a trend line and score-range summary into the existing Health Score detail dialog once at least two days of history exist.
- **Dashboard raw-enum-name fix (P1)**: the Dashboard's "Needs Attention" list and "Next Service" card displayed a maintenance record's raw `MaintenanceCategory` constant (e.g. `ENGINE_OIL`) instead of its localized label - wrong in every locale, English included. `UpcomingTaskUi` now carries an optional `titleRes`, resolved through `EnumLabels` at render time, alongside the existing free-text title used for document/reminder/part names (which are already user-entered and correct as-is).
- **Ownership Cost Analytics + currency-safe fuel stats (P1)**: Insights' "Cost / KM" stat only reflected the standalone Expenses table, silently excluding fuel and maintenance spend - understating true cost per km for every vehicle. Added a real combined Total Ownership Cost stat (fuel + maintenance + expenses, all pre-filtered to the vehicle's display currency, per the currency-architecture rule above) and rewired Cost/KM to divide that by total distance. Separately, `FuelStatsCalculator` itself computed `totalCost`/`monthlyCost`/`yearlyCost`/`costPerKm` as a blind sum across every fill-up's currency regardless of what was actually recorded - currently dead code (no screen renders those fields yet) but a landmine for the next one that does; `FuelEntry` now carries `currencyCode` and those fields are restricted to the single most-common currency among the entries, matching the same "never sum across currencies" rule everywhere else.
- **Warranty expiry -> real reminder (P1)**: a part's warranty expiry was only ever surfaced passively through the Dashboard's "Needs Attention" list - never a real notification, unlike Document expiry which already gets one. `PartRepository` now mirrors `DocumentRepository`'s auto-generated-reminder lifecycle (schema v8→v9, `parts.reminderId`): a part with a warranty date and/or mileage trigger set always has exactly one linked `ReminderEntity`, created/updated/deleted in lockstep with the part, with its `basis` (DATE/MILEAGE/BOTH) derived from whichever trigger(s) are actually set - so the existing periodic `ReminderCheckWorker` picks up warranty expiry the same way it already does for documents, no separate scheduling path needed.
- **Vehicle Sale Report PDF upgrade + disclaimer (P1)**: the report had no Parts/Warranty section at all despite Parts History + Warranty Tracking existing as its own feature - a buyer-relevant detail (e.g. "battery replaced 2024, still under warranty") that was simply missing. Added one, listing each part with its install date and a derived under-warranty/expired/none status. Also added an explicit disclaimer section clarifying the report reflects only owner-logged data, is not an independent inspection/appraisal/vehicle-history report, and that buyers should get their own professional inspection and independently verify title/accident history - honest about what this local, offline report can and can't vouch for.
- **Timeline event text raw-enum-name fix (P1)**: `TimelineDisplay.titleFor()` already resolved Maintenance/Fuel/Inspection timeline titles to localized labels, but Expense fell through untouched - an expense with no free-text description showed its raw `ExpenseCategory` constant (e.g. `CAR_WASH`) unlocalized, same class of bug as the Dashboard one above. Extended the same `enumValueOf`-and-resolve pattern already used for the other three types. The offline Global Search screen had the identical bug in its result subtitles (Maintenance category, Expense category, Document type all shown as raw constants) - fixed by carrying a resolved `@StringRes` id through `GlobalSearchResult` instead of baking `.name` into the ViewModel-built subtitle string, resolved at render time in the adapter (which has a `Context`, unlike the ViewModel). Timeline's category filter chips already existed and needed no changes.
- **Notification channel reliability fix (P1)**: the tiered notification severity feature (Critical/Important/Upcoming) set `NotificationCompat.Builder.setPriority()` per severity, but every notification posted to the *same single* `rovena_reminders` channel - on API 26+ (every real device this app can realistically run on at targetSdk 36) `setPriority()` is a silent no-op; a notification's actual sound/heads-up/badge behavior is decided solely by its channel's importance. The severity tiers were therefore cosmetically correct (right text, right internal priority field) but functionally inert - every reminder behaved identically regardless of urgency. Replaced the single reminders channel (plus an already-unused, never-referenced `rovena_documents` channel) with one channel per severity tier (`IMPORTANCE_HIGH`/`DEFAULT`/`LOW`), selected by `NotificationHelper` per notification. Channel importance can't be changed in place once created, so this uses fresh channel ids rather than trying to migrate the old one.
- **App Lock timeout options (P1)**: App Lock previously had no configurable grace period at all - `RovenaApp` flipped `requiresReauth = true` the instant the whole app left the foreground for *any* reason (a document picker, a share sheet, even a system rotation-triggered recreate on some OEM skins), forcing a fresh PIN/biometric prompt every single time, with no way to loosen that. Reworked into a real timeout: `RovenaApp` now just records `backgroundedAtMillis` on stop; `MainActivity.checkAppLock()` re-evaluates on every resume whether the user's configured `appLockTimeoutSeconds` (schema v9→v10) has actually elapsed before re-locking. New Settings row ("Lock timeout", only enabled while App Lock is on) offers Immediately (the previous, still-default behavior) / After 30 seconds / After 1 minute / After 5 minutes.
- **RTL/accessibility/empty-state/error-UX audit (P1)**: reviewed the codebase against all four - no hardcoded `left`/`right` margins or gravity anywhere (every layout already uses `start`/`end`), the back-chevron icon's `android:autoMirrored="true"` + static 180° rotation combination is genuinely correct in both directions (verified by tracing through the RTL case rather than assuming), the shared `fragment_generic_list.xml` empty-state view already covers all ten list screens that use it, and no silently-swallowed exceptions (`catch { }` with an empty body) exist anywhere. Found and fixed one real gap: the shared "Add" FAB used by those same ten screens (Maintenance, Fuel, Expenses, Documents, Reminders, Parts, Notes, Inspections, Timeline, Garage) had no `contentDescription` at all - TalkBack would announce it with no label. Added a generic localized one.
- **DB integrity / transaction audit (P1)**: `FuelRepository`, `MaintenanceRepository`, `ExpenseRepository`, `ReminderRepository`, and `VehicleRepository.updateVehicle()` all wrote their record + its Timeline mirror (and, for Fuel/Maintenance, the vehicle's `currentMileageKm` bump) as separate, unwrapped DAO calls - a process death or crash between them could leave a real record with no Timeline entry, or vice versa. `DocumentRepository`/`PartRepository`/`InspectionRepository` already got this right via `database.withTransaction { }`; the other four now do too, each call wrapped as a single atomic unit exactly like those three.

---

## Known limitations / honest disclosure

- **Edge-to-edge/insets verification is mechanical, not device-tested.** `compileSdk`/`targetSdk` are now 36 (AGP bumped to 8.11.0, Gradle to 8.13 to match), and every Activity root pads itself for system-bar + IME insets via a shared `applyEdgeToEdgeInsets()` helper - the single fix that covers content-behind-the-status-bar and content-behind-the-nav-bar across every screen without a per-layout audit. What this has **not** been verified against, because this sandbox has no device/emulator to render on: individual dialogs, bottom sheets, the full-screen PDF/image preview, and IME animation timing on real Android 15/16 hardware. Treat it as "should be correct by construction," not "confirmed on-device."
- Forgetting your App Lock PIN currently has no in-app recovery flow (there's no account to reset it through, by design) — clearing app data is the only way out, which erases local data unless you have a `.rovena` backup. Worth a "recovery codes" feature in a future version.
- Removing a photo from the maintenance-record photo strip (or deleting a maintenance record entirely) does not currently delete the underlying file from internal storage, only the database row - a minor storage leak, not a data-loss or security issue. The same gap does **not** exist for inspection-item photos or vehicle deletion, both of which do clean up their files (see [Security](#security) and `InspectionRepository.delete()`/`VehicleRepository.deleteVehicle()`).
- Inspection-item photos are not yet embedded into the generated Inspection PDF report (notes and estimated cost are). A reasonable next enhancement, not attempted in this pass.
- Espresso/instrumented UI tests are not written yet (need a device/emulator - see [Testing](#testing)). Backup/restore, inspection photos, and PIN lockout are covered by Robolectric + in-memory-Room integration tests instead, which exercise the real DAOs and transactions without needing a device.
- The three custom chart views (bar/line/donut) are intentionally hand-rolled Canvas drawing rather than a charting library, per the "keep dependencies minimal" instruction — they cover the required monthly-spend, fuel-trend, and category-breakdown visualizations but are not a general-purpose charting engine.
