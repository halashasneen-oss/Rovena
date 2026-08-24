package com.rovena.garage.presentation.documents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentDocumentFormBinding
import com.rovena.garage.domain.model.DocumentType
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.confirmDelete
import com.rovena.garage.presentation.common.guardUnsavedChanges
import com.rovena.garage.presentation.common.setOnDebouncedClickListener
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class DocumentFormFragment : Fragment(R.layout.fragment_document_form) {

    private var _binding: FragmentDocumentFormBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong("vehicleId", 0L) ?: 0L }
    private val recordId: Long by lazy { arguments?.getLong("recordId", 0L) ?: 0L }

    private val viewModel: DocumentFormViewModel by viewModels {
        viewModelFactory { DocumentFormViewModel(appContainer, vehicleId, recordId) }
    }

    private var isBinding = false
    private var isDirty = false

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val mimeType = requireContext().contentResolver.getType(uri)
        val extension = if (mimeType == "application/pdf") "pdf" else "jpg"
        val destDir = File(requireContext().filesDir, "documents").apply { mkdirs() }
        val destFile = File(destDir, "${UUID.randomUUID()}.$extension")
        runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { input -> destFile.outputStream().use { input.copyTo(it) } }
        }.onSuccess {
            isDirty = true
            viewModel.update { it.copy(filePath = destFile.absolutePath, mimeType = mimeType) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocumentFormBinding.bind(inflater.inflate(R.layout.fragment_document_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typeLabels = DocumentType.values().map { getString(EnumLabels.of(it)) }
        binding.typeDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, typeLabels))
        binding.typeDropdown.setOnItemClickListener { _, _, position, _ ->
            isDirty = true
            viewModel.update { it.copy(type = DocumentType.values()[position]) }
        }

        binding.issueDateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "doc_issue", viewModel.state.value.issueDateMillis) { millis ->
                isDirty = true
                viewModel.update { it.copy(issueDateMillis = millis) }
            }
        }
        binding.expiryDateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "doc_expiry", viewModel.state.value.expiryDateMillis) { millis ->
                isDirty = true
                viewModel.update { it.copy(expiryDateMillis = millis) }
            }
        }

        binding.attachButton.setOnClickListener { pickFile.launch("*/*") }

        wire(binding.nameInput) { viewModel.update { s -> s.copy(name = it) } }
        wire(binding.notesInput) { viewModel.update { s -> s.copy(notes = it) } }

        binding.saveButton.setOnDebouncedClickListener { viewModel.save() }
        binding.deleteButton.setOnDebouncedClickListener { confirmDelete { viewModel.delete() } }
        guardUnsavedChanges { isDirty }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: DocumentFormState) {
        if (state.isLoading) return
        isBinding = true
        setIfChanged(binding.nameInput, state.name)
        setIfChanged(binding.notesInput, state.notes)
        binding.typeDropdown.setText(getString(EnumLabels.of(state.type)), false)
        isBinding = false

        binding.issueDateButton.text = state.issueDateMillis?.let { Formatters.date(requireContext(), it) } ?: getString(R.string.document_field_issue_date)
        binding.expiryDateButton.text = state.expiryDateMillis?.let { Formatters.date(requireContext(), it) } ?: getString(R.string.document_field_expiry_date)
        binding.fileStatusText.text = if (state.filePath != null) getString(R.string.document_field_file_attached) else getString(R.string.document_field_no_file)

        binding.nameLayout.error = state.errors["name"]?.let { getString(it) }
        binding.deleteButton.visibility = if (state.id != 0L) View.VISIBLE else View.GONE

        if (state.isSaved || state.isDeleted) findNavController().popBackStack()
    }

    private fun wire(input: com.google.android.material.textfield.TextInputEditText, onChanged: (String) -> Unit) {
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isBinding) {
                    isDirty = true
                    onChanged(s?.toString().orEmpty())
                }
            }
        })
    }

    private fun setIfChanged(input: com.google.android.material.textfield.TextInputEditText, value: String) {
        if (input.text?.toString() != value) input.setText(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
