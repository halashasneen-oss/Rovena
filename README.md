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
Rovena now calculates estimated fuel efficiency from consecutive fill-ups, including km/L, L/100 km, fuel cost per km, average price per liter and recent efficiency direction. A data-based care score summarizes overdue/upcoming maintenance, expired/expiring documents and meaningful fuel-efficiency deterioration with an explicit confidence level.

Smart vehicle reminders now prioritize urgent overdue maintenance and expired documents, then approaching care items, and can surface a significant fuel-efficiency drop when enough fill-up history exists. Repeated alerts are rate-limited and deduplicated so the reminder system stays useful instead of noisy.

The new Insights area exposes the care score, fuel analytics and document status. The score is deliberately described as a record-based care indicator, not a mechanical diagnosis or replacement for a physical inspection.

## Engineering baseline
- Application ID: `com.rovena.garage`
- Kotlin + Jetpack Compose + Material 3
- Room for durable local vehicle data
- DataStore for preferences and reminder deduplication
- WorkManager for engagement and smart vehicle-care reminders
- Android 26+; target/compile SDK 36
- GitHub Actions runs unit tests, builds the debug APK and runs Android lint on every push

Next: richer maintenance plans by vehicle profile, warning-light guidance, document/file capture, export/backup and stronger per-vehicle reports.
