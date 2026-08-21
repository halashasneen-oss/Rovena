package com.rovena.garage.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.domain.model.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentFormState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val name: String = "",
    val type: DocumentType = DocumentType.OTHER,
    val issueDateMillis: Long? = null,
    val expiryDateMillis: Long? = null,
    val notes: String = "",
    val filePath: String? = null,
    val mimeType: String? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val savedReminderId: Long? = null,
    val errors: Map<String, Int> = emptyMap()
)

class DocumentFormViewModel(private val container: AppContainer, private val vehicleId: Long, private val recordId: Long) : ViewModel() {

    private val _state = MutableStateFlow(DocumentFormState(vehicleId = vehicleId, isLoading = recordId != 0L))
    val state: StateFlow<DocumentFormState> = _state.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                container.documentRepository.getById(recordId)?.let { d ->
                    _state.value = DocumentFormState(
                        id = d.id, vehicleId = d.vehicleId, name = d.name, type = d.type,
                        issueDateMillis = d.issueDateMillis, expiryDateMillis = d.expiryDateMillis,
                        notes = d.notes.orEmpty(), filePath = d.filePath, mimeType = d.mimeType, isLoading = false
                    )
                } ?: run { _state.value = _state.value.copy(isLoading = false) }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun update(transform: (DocumentFormState) -> DocumentFormState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, Int>()
        if (s.name.isBlank()) errors["name"] = R.string.error_required
        if (s.filePath == null) errors["file"] = R.string.error_file_required
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }
        viewModelScope.launch {
            val entity = DocumentEntity(
                id = s.id, vehicleId = s.vehicleId, name = s.name.trim(), type = s.type,
                issueDateMillis = s.issueDateMillis, expiryDateMillis = s.expiryDateMillis,
                notes = s.notes.trim().ifBlank { null }, filePath = s.filePath!!, mimeType = s.mimeType
            )
            val (_, reminderId) = container.documentRepository.addOrUpdate(entity)
            _state.value = _state.value.copy(isSaved = true, savedReminderId = reminderId, errors = emptyMap())
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) return
        viewModelScope.launch {
            container.documentRepository.getById(s.id)?.let { container.documentRepository.delete(it) }
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
