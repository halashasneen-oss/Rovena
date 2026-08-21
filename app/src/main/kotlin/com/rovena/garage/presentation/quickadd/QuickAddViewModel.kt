package com.rovena.garage.presentation.quickadd

import androidx.lifecycle.ViewModel
import com.rovena.garage.AppContainer

class QuickAddViewModel(private val container: AppContainer) : ViewModel() {
    suspend fun addNote(vehicleId: Long, text: String) {
        container.timelineRepository.addNote(vehicleId, text)
    }
}
