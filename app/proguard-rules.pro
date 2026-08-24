# Room
-keep class com.rovena.garage.data.local.entities.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# No reflection-based ViewModel factories are used (see presentation/common/ViewModelFactory.kt),
# so nothing here needs member names kept - only class names, so a crash stack trace pulled over
# ADB still reads as "MaintenanceFormViewModel" rather than an obfuscated single letter, while
# R8 can still rename/shrink individual methods and fields within these classes.
-keepnames class com.rovena.garage.presentation.**
