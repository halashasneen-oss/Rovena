# Room
-keep class com.rovena.garage.data.local.entities.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep ViewModel constructors used via reflection-free factories (none needed, but keep names for debugging)
-keepnames class com.rovena.garage.presentation.** { *; }
