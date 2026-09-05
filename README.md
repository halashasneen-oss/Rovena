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
