package com.rovena.garage.presentation.expenses

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
import com.rovena.garage.databinding.FragmentExpenseFormBinding
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ExpenseFormFragment : Fragment(R.layout.fragment_expense_form) {

    private var _binding: FragmentExpenseFormBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong("vehicleId", 0L) ?: 0L }
    private val recordId: Long by lazy { arguments?.getLong("recordId", 0L) ?: 0L }

    private val viewModel: ExpenseFormViewModel by viewModels {
        viewModelFactory { ExpenseFormViewModel(appContainer, vehicleId, recordId) }
    }

    private var isBinding = false

    private val pickReceipt = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val destDir = File(requireContext().filesDir, "receipts").apply { mkdirs() }
        val destFile = File(destDir, "${UUID.randomUUID()}.jpg")
        runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { input -> destFile.outputStream().use { input.copyTo(it) } }
        }.onSuccess { viewModel.update { it.copy(receiptPhotoPath = destFile.absolutePath) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpenseFormBinding.bind(inflater.inflate(R.layout.fragment_expense_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryLabels = ExpenseCategory.values().map { getString(EnumLabels.of(it)) }
        binding.categoryDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoryLabels))
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.update { it.copy(category = ExpenseCategory.values()[position]) }
        }

        binding.dateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "expense_date", viewModel.state.value.dateMillis) { millis ->
                viewModel.update { it.copy(dateMillis = millis) }
            }
        }

        binding.receiptButton.setOnClickListener { pickReceipt.launch("image/*") }

        wire(binding.amountInput) { viewModel.update { s -> s.copy(amount = it) } }
        wire(binding.descriptionInput) { viewModel.update { s -> s.copy(description = it) } }
        wire(binding.mileageInput) { viewModel.update { s -> s.copy(mileage = it) } }
        wire(binding.vendorInput) { viewModel.update { s -> s.copy(vendor = it) } }
        wire(binding.notesInput) { viewModel.update { s -> s.copy(notes = it) } }

        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener { viewModel.delete() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ExpenseFormState) {
        if (state.isLoading) return
        isBinding = true
        setIfChanged(binding.amountInput, state.amount)
        setIfChanged(binding.descriptionInput, state.description)
        setIfChanged(binding.mileageInput, state.mileage)
        setIfChanged(binding.vendorInput, state.vendor)
        setIfChanged(binding.notesInput, state.notes)
        binding.categoryDropdown.setText(getString(EnumLabels.of(state.category)), false)
        binding.dateButton.text = Formatters.date(requireContext(), state.dateMillis)
        isBinding = false

        binding.receiptButton.text = if (state.receiptPhotoPath != null) getString(R.string.expense_field_receipt_attached) else getString(R.string.expense_field_receipt)
        binding.amountLayout.error = state.errors["amount"]?.let { getString(it) }
        binding.deleteButton.visibility = if (state.id != 0L) View.VISIBLE else View.GONE

        if (state.isSaved || state.isDeleted) findNavController().popBackStack()
    }

    private fun wire(input: com.google.android.material.textfield.TextInputEditText, onChanged: (String) -> Unit) {
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isBinding) onChanged(s?.toString().orEmpty())
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
