package com.rovena.garage.presentation.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentReminderFormBinding
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch

class ReminderFormFragment : Fragment(R.layout.fragment_reminder_form) {

    private var _binding: FragmentReminderFormBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong("vehicleId", 0L) ?: 0L }
    private val recordId: Long by lazy { arguments?.getLong("recordId", 0L) ?: 0L }

    private val viewModel: ReminderFormViewModel by viewModels {
        viewModelFactory { ReminderFormViewModel(appContainer, vehicleId, recordId) }
    }

    private var isBinding = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReminderFormBinding.bind(inflater.inflate(R.layout.fragment_reminder_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val basisLabels = ReminderBasis.values().map { getString(EnumLabels.of(it)) }
        binding.basisDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, basisLabels))
        binding.basisDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.update { it.copy(basis = ReminderBasis.values()[position]) }
        }

        binding.dueDateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "reminder_due_date", viewModel.state.value.dueDateMillis) { millis ->
                viewModel.update { it.copy(dueDateMillis = millis) }
            }
        }

        binding.recurringSwitch.setOnCheckedChangeListener { _, checked ->
            binding.intervalContainer.visibility = if (checked) View.VISIBLE else View.GONE
            if (!isBinding) viewModel.update { it.copy(isRecurring = checked) }
        }

        wire(binding.titleInput) { viewModel.update { s -> s.copy(title = it) } }
        wire(binding.dueMileageInput) { viewModel.update { s -> s.copy(dueMileage = it) } }
        wire(binding.intervalKmInput) { viewModel.update { s -> s.copy(intervalKm = it) } }
        wire(binding.intervalMonthsInput) { viewModel.update { s -> s.copy(intervalMonths = it) } }
        wire(binding.notesInput) { viewModel.update { s -> s.copy(notes = it) } }

        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener { viewModel.delete() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ReminderFormState) {
        if (state.isLoading) return
        isBinding = true
        setIfChanged(binding.titleInput, state.title)
        setIfChanged(binding.dueMileageInput, state.dueMileage)
        setIfChanged(binding.intervalKmInput, state.intervalKm)
        setIfChanged(binding.intervalMonthsInput, state.intervalMonths)
        setIfChanged(binding.notesInput, state.notes)
        binding.basisDropdown.setText(getString(EnumLabels.of(state.basis)), false)
        binding.recurringSwitch.isChecked = state.isRecurring
        binding.intervalContainer.visibility = if (state.isRecurring) View.VISIBLE else View.GONE
        isBinding = false

        binding.dueMileageLayout.visibility = if (state.basis != ReminderBasis.DATE) View.VISIBLE else View.GONE
        binding.dueDateButton.visibility = if (state.basis != ReminderBasis.MILEAGE) View.VISIBLE else View.GONE
        binding.dueDateButton.text = state.dueDateMillis?.let { Formatters.date(requireContext(), it) } ?: getString(R.string.reminder_field_due_date)

        binding.titleLayout.error = state.errors["title"]?.let { getString(it) }
        binding.triggerError.visibility = if (state.errors.containsKey("trigger")) View.VISIBLE else View.GONE
        binding.triggerError.text = state.errors["trigger"]?.let { getString(it) }
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
