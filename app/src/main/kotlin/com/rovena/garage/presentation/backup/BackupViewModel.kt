package com.rovena.garage.presentation.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.BackupMetadataEntity
import com.rovena.garage.domain.usecase.BackupVersionValidator
import com.rovena.garage.utils.backup.BackupInspection
import com.rovena.garage.utils.backup.BackupManager
import com.rovena.garage.utils.backup.BackupResult
import com.rovena.garage.utils.backup.BackupStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupUiEvent {
    data class BackupCreated(val vehicleCount: Int) : BackupUiEvent()
    data class BackupError(val message: String) : BackupUiEvent()
    data class RestorePending(val inspection: BackupInspection) : BackupUiEvent()
    data class RestoreInvalid(val validation: BackupVersionValidator.ValidationResult) : BackupUiEvent()
    /** [wrongPasswordRetry] distinguishes the first prompt from a retry after an incorrect attempt, so the Fragment can show an inline error only on the retry. */
    data class RestorePasswordNeeded(val source: Uri, val wrongPasswordRetry: Boolean) : BackupUiEvent()
    object RestoreCompletedNeedsRestart : BackupUiEvent()
    data class RestoreCompletedMerged(val vehicleCount: Int) : BackupUiEvent()
}

class BackupViewModel(private val container: AppContainer) : ViewModel() {

    val history: StateFlow<List<BackupMetadataEntity>> = container.backupMetadataRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableStateFlow<BackupUiEvent?>(null)
    val events: StateFlow<BackupUiEvent?> = _events.asStateFlow()

    /** Current stage of an in-flight backup/restore operation, or null when idle - drives a non-dismissible progress dialog so a multi-second operation never looks frozen (spec: backup security). */
    private val _progress = MutableStateFlow<BackupStage?>(null)
    val progress: StateFlow<BackupStage?> = _progress.asStateFlow()

    fun consumeEvent() {
        _events.value = null
    }

    fun createBackup(context: Context, destination: Uri, password: String? = null) {
        viewModelScope.launch {
            val result = BackupManager.createBackup(context, container, destination, password) { _progress.value = it }
            _progress.value = null
            when (result) {
                is BackupResult.Success -> _events.value = BackupUiEvent.BackupCreated(result.vehicleCount)
                is BackupResult.Error -> _events.value = BackupUiEvent.BackupError(result.message)
                is BackupResult.Invalid -> Unit
            }
        }
    }

    fun inspectBackup(context: Context, source: Uri, password: String? = null) {
        viewModelScope.launch {
            val inspection = BackupManager.inspect(context, source, password) { _progress.value = it }
            _progress.value = null
            when (inspection.validation) {
                BackupVersionValidator.ValidationResult.Valid -> _events.value = BackupUiEvent.RestorePending(inspection)
                BackupVersionValidator.ValidationResult.PasswordRequired ->
                    _events.value = BackupUiEvent.RestorePasswordNeeded(source, wrongPasswordRetry = false)
                BackupVersionValidator.ValidationResult.WrongPassword ->
                    _events.value = BackupUiEvent.RestorePasswordNeeded(source, wrongPasswordRetry = true)
                else -> _events.value = BackupUiEvent.RestoreInvalid(inspection.validation)
            }
        }
    }

    fun restoreReplacing(context: Context, inspection: BackupInspection) {
        val dir = inspection.extractedDir ?: return
        viewModelScope.launch {
            val result = BackupManager.restoreReplacing(context, dir) { _progress.value = it }
            _progress.value = null
            when (result) {
                is BackupResult.Success -> _events.value = BackupUiEvent.RestoreCompletedNeedsRestart
                is BackupResult.Error -> _events.value = BackupUiEvent.BackupError(result.message)
                is BackupResult.Invalid -> Unit
            }
        }
    }

    fun restoreAsNewGarage(context: Context, inspection: BackupInspection) {
        val dir = inspection.extractedDir ?: return
        viewModelScope.launch {
            val result = BackupManager.restoreAsNewGarage(context, container, dir) { _progress.value = it }
            _progress.value = null
            when (result) {
                is BackupResult.Success -> {
                    container.backupMetadataRepository.record(
                        BackupMetadataEntity(
                            fileName = "restored", type = com.rovena.garage.domain.model.BackupRecordType.RESTORED,
                            backupFormatVersion = BackupVersionValidator.CURRENT_BACKUP_FORMAT_VERSION,
                            vehicleCount = result.vehicleCount, sizeBytes = 0
                        )
                    )
                    _events.value = BackupUiEvent.RestoreCompletedMerged(result.vehicleCount)
                }
                is BackupResult.Error -> _events.value = BackupUiEvent.BackupError(result.message)
                is BackupResult.Invalid -> Unit
            }
        }
    }
}
