package com.rovena.garage.presentation.common

import androidx.fragment.app.Fragment
import com.rovena.garage.AppContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Every list/detail screen in Rovena can be reached two ways: from the
 * bottom-nav SERVICE/INSIGHTS tabs (no specific vehicle - use whichever is
 * "current"), or from the Garage / Vehicle Hub with an explicit vehicle
 * already chosen. This resolves both cases into one reactive vehicle id.
 */
fun Fragment.resolveVehicleId(container: AppContainer): Flow<Long?> {
    val argId = arguments?.getLong("vehicleId", 0L)?.takeIf { it != 0L }
    return if (argId != null) flowOf(argId) else container.userPreferences.currentVehicleId
}
