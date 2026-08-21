package com.rovena.garage.presentation.maintenance

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
import com.rovena.garage.databinding.FragmentMaintenanceFormBinding
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.presentation.common.PhotoStripController
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch

class MaintenanceFormFragment : Fragment(R.layout.fragment_maintenance_form) {

    private var _binding: FragmentMaintenanceFormBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong("vehicleId", 0L) ?: 0L }
    private val recordId: Long by lazy { arguments?.getLong("recordId", 0L) ?: 0L }

    private val viewModel: MaintenanceFormViewModel by viewModels {
        viewModelFactory { MaintenanceFormViewModel(appContainer, vehicleId, recordId) }
    }

    private val photoController = PhotoStripController(this) { paths -> viewModel.update { it.copy(photoPaths = paths) } }

    private var isBinding = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMaintenanceFormBinding.bind(inflater.inflate(R.layout.fragment_maintenance_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryLabels = MaintenanceCategory.values().map { getString(EnumLabels.of(it)) }
        binding.categoryDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoryLabels))
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.update { it.copy(category = MaintenanceCategory.values()[position]) }
        }

        binding.dateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "maintenance_date", viewModel.state.value.dateMillis) { millis ->
                viewModel.update { it.copy(dateMillis = millis) }
            }
        }
        binding.nextDueDateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "maintenance_next_due", viewModel.state.value.nextDueDateMillis) { millis ->
                viewModel.update { it.copy(nextDueDateMillis = millis) }
            }
        }

        wire(binding.mileageInput) { viewModel.update { s -> s.copy(mileage = it) } }
        wire(binding.descriptionInput) { viewModel.update { s -> s.copy(description = it) } }
        wire(binding.costInput) { viewModel.update { s -> s.copy(cost = it) } }
        wire(binding.partsInput) { viewModel.update { s -> s.copy(parts = it) } }
        wire(binding.workshopInput) { viewModel.update { s -> s.copy(workshop = it) } }
        wire(binding.technicianInput) { viewModel.update { s -> s.copy(technician = it) } }
        wire(binding.notesInput) { viewModel.update { s -> s.copy(notes = it) } }
        wire(binding.nextDueMileageInput) { viewModel.update { s -> s.copy(nextDueMileage = it) } }

        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener { viewModel.delete() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: MaintenanceFormState) {
        if (state.isLoading) return
        isBinding = true
        setIfChanged(binding.mileageInput, state.mileage)
        setIfChanged(binding.descriptionInput, state.description)
        setIfChanged(binding.costInput, state.cost)
        setIfChanged(binding.partsInput, state.parts)
        setIfChanged(binding.workshopInput, state.workshop)
        setIfChanged(binding.technicianInput, state.technician)
        setIfChanged(binding.notesInput, state.notes)
        setIfChanged(binding.nextDueMileageInput, state.nextDueMileage)
        binding.categoryDropdown.setText(getString(EnumLabels.of(state.category)), false)
        binding.dateButton.text = Formatters.date(requireContext(), state.dateMillis)
        binding.nextDueDateButton.text = state.nextDueDateMillis?.let { Formatters.date(requireContext(), it) }
            ?: getString(R.string.maintenance_field_next_due_date)
        isBinding = false

        if (!boundOnce && !state.isLoading) {
            boundOnce = true
            photoController.bind(binding.photoStripContainer, state.photoPaths)
        }

        binding.mileageLayout.error = state.errors["mileage"]?.let { getString(it) }
        binding.descriptionLayout.error = state.errors["description"]?.let { getString(it) }
        binding.deleteButton.visibility = if (state.id != 0L) View.VISIBLE else View.GONE

        if (state.isSaved || state.isDeleted) {
            findNavController().popBackStack()
        }
    }

    private var boundOnce = false

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
