package com.rovena.garage.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rovena_prefs")

/**
 * Fast, purely-local UI state that doesn't belong in the Room database:
 * which vehicle is currently selected, and how far the user got through
 * onboarding. Kept separate from [AppSettingsEntity] so switching vehicles
 * doesn't generate churn in the backed-up settings table.
 */
class UserPreferences(private val context: Context) {

    private object Keys {
        val CURRENT_VEHICLE_ID = longPreferencesKey("current_vehicle_id")
        val ONBOARDING_STEP = stringPreferencesKey("onboarding_step")
    }

    val currentVehicleId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_VEHICLE_ID]?.takeIf { it > 0 }
    }

    suspend fun setCurrentVehicleId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.CURRENT_VEHICLE_ID) else prefs[Keys.CURRENT_VEHICLE_ID] = id
        }
    }
}
