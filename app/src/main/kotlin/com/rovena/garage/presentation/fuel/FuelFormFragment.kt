package com.rovena.garage.presentation.fuel

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
import com.rovena.garage.databinding.FragmentFuelFormBinding
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch

class FuelFormFragment : Fragment(R.layout.fragment_fuel_form) {

    private var _binding: FragmentFuelFormBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong("vehicleId", 0L) ?: 0L }
    private val recordId: Long by lazy { arguments?.getLong("recordId", 0L) ?: 0L }

    private val viewModel: FuelFormViewModel by viewModels {
        viewModelFactory { FuelFormViewModel(appContainer, vehicleId, recordId) }
    }

    private var isBinding = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFuelFormBinding.bind(inflater.inflate(R.layout.fragment_fuel_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fuelLabels = FuelType.values().map { getString(EnumLabels.of(it)) }
        binding.fuelTypeDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, fuelLabels))
        binding.fuelTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.update { it.copy(fuelType = FuelType.values()[position]) }
        }

        binding.dateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "fuel_date", viewModel.state.value.dateMillis) { millis ->
                viewModel.update { it.copy(dateMillis = millis) }
            }
        }

        wire(binding.mileageInput) { viewModel.update { s -> s.copy(mileage = it) } }
        wire(binding.litersInput) { viewModel.update { s -> s.copy(liters = it) }; viewModel.recomputeTotal() }
        wire(binding.priceInput) { viewModel.update { s -> s.copy(pricePerLiter = it) }; viewModel.recomputeTotal() }
        wire(binding.totalCostInput) { viewModel.update { s -> s.copy(totalCost = it) } }
        wire(binding.stationInput) { viewModel.update { s -> s.copy(station = it) } }
        wire(binding.notesInput) { viewModel.update { s -> s.copy(notes = it) } }

        binding.fullTankSwitch.setOnCheckedChangeListener { _, checked -> if (!isBinding) viewModel.update { it.copy(isFullTank = checked) } }

        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener { viewModel.delete() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: FuelFormState) {
        if (state.isLoading) return
        isBinding = true
        setIfChanged(binding.mileageInput, state.mileage)
        setIfChanged(binding.litersInput, state.liters)
        setIfChanged(binding.priceInput, state.pricePerLiter)
        setIfChanged(binding.totalCostInput, state.totalCost)
        setIfChanged(binding.stationInput, state.station)
        setIfChanged(binding.notesInput, state.notes)
        binding.fuelTypeDropdown.setText(getString(EnumLabels.of(state.fuelType)), false)
        binding.fullTankSwitch.isChecked = state.isFullTank
        binding.dateButton.text = Formatters.date(requireContext(), state.dateMillis)
        isBinding = false

        val mileageWarningText = state.mileageWarning?.let { Formatters.mileageWarningText(requireContext(), it) }
        binding.mileageWarning.text = mileageWarningText
        binding.mileageWarning.visibility = if (mileageWarningText != null) View.VISIBLE else View.GONE
        binding.mileageLayout.error = state.errors["mileage"]?.let { getString(it) }
        binding.litersLayout.error = state.errors["liters"]?.let { getString(it) }
        binding.totalCostLayout.error = state.errors["totalCost"]?.let { getString(it) }
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
