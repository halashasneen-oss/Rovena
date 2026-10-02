# Rovena 2

Clean rebuild of Rovena as a modern smart car companion.

## Product direction
- Premium automotive experience
- Multi-car digital garage
- Maintenance, fuel, expenses and documents
- Data-based vehicle care insights without pretending to perform a mechanical diagnosis
- Smart maintenance/document/fuel reminders plus non-spammy return reminders
- Arabic, English, French, Spanish, German and Turkish
- Full RTL support for Arabic

## Milestone 1 — persistent garage
Rovena now has a Room-backed multi-car garage. Users can add cars, keep make/model/year/odometer/fuel/plate/VIN/currency details, choose the current vehicle and safely delete vehicles.

## Milestone 2 — persistent vehicle records
Maintenance, fuel, expenses and documents are first-class records linked to each vehicle. New fuel or maintenance odometer readings can advance the saved vehicle odometer, costs feed the expense dashboard, and scheduled maintenance/document dates drive local reminders.

## Milestone 3 — smart insights
Rovena calculates estimated fuel efficiency from consecutive fill-ups, including km/L, L/100 km, fuel cost per km, average price per liter and recent efficiency direction. A data-based care score summarizes overdue/upcoming maintenance, expired/expiring documents and meaningful fuel-efficiency deterioration with an explicit confidence level.

## Milestone 4 — smart car guidance
The Smart Center now has a dedicated Car Tools area alongside the existing insights overview. It provides a mileage-based maintenance guide that can use matching recorded services, adjusts combustion-only tasks for electric vehicles, explains high-priority dashboard warning lights, and offers conservative guidance for common vehicle symptoms.

The maintenance intervals are explicitly generic guidance, not manufacturer-specific schedules. Warning-light and symptom guidance is safety-oriented and always presented as information rather than a mechanical diagnosis.

## Engineering baseline
- Application ID: `com.rovena.garage`
- Kotlin + Jetpack Compose + Material 3
- Room for durable local vehicle data
- DataStore for preferences and reminder deduplication
- WorkManager for engagement and smart vehicle-care reminders
- Android 26+; target/compile SDK 36
- GitHub Actions runs unit tests, builds the debug APK and runs Android lint on every push

Next: richer manufacturer/model-specific schedules, document/file capture, export/backup, reports, settings and release polish.

## Version 2.1 — Value Boost / Premium Automotive UI
- Deep-navy and teal premium theme across garage, dashboard, history, expenses and forms.
- Interactive care ring with confidence, three contextual priorities and data-based actions.
- Expense Radar: lifetime spend, monthly comparison, fuel/maintenance/other breakdown and cost/km when enough odometer data exists.
- Digital Vehicle Passport: branded multi-page PDF with vehicle details, spending and maintenance/fuel/document history.
- Restore requires explicit confirmation; JSON backups are validated before transactional replacement. Backups contain attachment URIs, not binary files.
- Unknown Room database versions no longer trigger silent destructive migration.
- Release bundle is signed only when the ROVENA_* signing secrets are available.

## Rovena AdMob integration — version 2.2

- Debug APK: **only official Google demo ads** (anchored adaptive banner, capped interstitial, opt-in rewarded ad).
- Live release: reads these five `GitHub Actions > Secrets and variables > Actions` secrets, created specifically for the **Rovena** app:
  - `ROVENA_ADMOB_APP_ID` in the form `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`
  - `ROVENA_ADMOB_BANNER_ID` in the form `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`
  - `ROVENA_ADMOB_INTERSTITIAL_ID` (same unit-ID format)
  - `ROVENA_ADMOB_REWARDED_ID` (same unit-ID format)
  - `ROVENA_ADMOB_APP_OPEN_ID` (same format; a separate App Open ad unit for Rovena)
- Do **not** copy ad unit IDs from Truth Test or another app. All five IDs must share the same publisher prefix.
- If any ID is missing, Gradle produces an ads-disabled release, with an explicit CI warning; no test ads are served in that release.
- For local release builds, export the same five environment variables. Run `./gradlew :app:verifyProductionAds` to assert that real IDs are set before publishing.
- Create and publish the appropriate **Privacy & messaging** consent form in the Rovena AdMob app; production UMP runs at every app launch after onboarding.
- Add `app-ads.txt` to the root of the **website listed in the Google Play store page** and verify it in AdMob. This file is external to the APK.
- Check Play Console's **Contains ads**, **Data safety** (ad identifiers / SDK network collection), and current privacy policy before publishing. Do not claim that this AdMob-enabled version is fully offline.
- Interstitial appears only after four completed record saves and no more often than once every five minutes, never on onboarding. A user may voluntarily earn one hour of banner/interstitial suppression by watching a rewarded ad.

### App Open (returning to Rovena)
- Uses the official App Open test ad ID in debug; needs `ROVENA_ADMOB_APP_OPEN_ID` in production.
- Preloads after SDK/UMP readiness; shows **only** on an eligible return after at least 90 seconds in background, never during onboarding, consent, or initial cold startup.
- Maximum once per 30 minutes, ignores ads older than four hours, respects rewarded ad-free time, and never interrupts active record-entry dialogs or another full-screen ad.
- App Open is skipped if a preloaded ad is unavailable; it must never appear unexpectedly after the user starts interacting.
