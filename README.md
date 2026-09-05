# Rovena 2

Clean rebuild of Rovena as a modern smart car companion.

## Product direction
- Premium automotive dashboard
- Multi-car digital garage
- Maintenance, fuel, expenses and documents
- Vehicle health insights based only on user-entered data
- Local maintenance/document reminders and non-spammy return reminders
- Arabic, English, French, Spanish, German and Turkish
- Full RTL support for Arabic

## Milestone 1 — persistent garage
The rebuild now has a new Room database and a real multi-car flow. Users can add vehicles, keep make/model/year/odometer/fuel/plate/VIN/currency details, choose the current vehicle and delete cars safely. The first car becomes the current vehicle automatically, and another vehicle is promoted if the current one is deleted.

The home dashboard is now backed by saved data rather than placeholders. It shows the selected vehicle, odometer, year and fuel type. The health area deliberately reports that more data is required until maintenance and inspection records exist; Rovena does not invent a mechanical health score.

## Engineering baseline
- Application ID: `com.rovena.garage`
- Kotlin + Jetpack Compose + Material 3
- Room for durable local garage data
- DataStore for app preferences
- WorkManager for engagement reminders
- Android 26+; target/compile SDK 36
- GitHub Actions runs unit tests, builds the debug APK and runs Android lint on every push

Next milestone: maintenance, fuel, expenses and documents will become persistent first-class records linked to each vehicle, followed by the real health and reminder engines.
