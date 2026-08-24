package com.rovena.garage.presentation.vehicleform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentVehicleFormBinding
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.TransmissionType
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.guardUnsavedChanges
import com.rovena.garage.presentation.common.setOnDebouncedClickListener
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.EnumLabels
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VehicleFormFragment : Fragment(R.layout.fragment_vehicle_form) {

    companion object {
        const val RESULT_KEY = "vehicle_form_result"
        const val RESULT_VEHICLE_ID = "vehicle_id"
        private const val ARG_VEHICLE_ID = "vehicleId"
        private const val ARG_ONBOARDING = "onboardingMode"

        fun newInstance(vehicleId: Long = 0L, onboardingMode: Boolean = false) = VehicleFormFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_VEHICLE_ID, vehicleId)
                putBoolean(ARG_ONBOARDING, onboardingMode)
            }
        }
    }

    private var _binding: FragmentVehicleFormBinding? = null
    private val binding get() = _binding!!

    private val navArgVehicleId: Long by lazy { arguments?.getLong(ARG_VEHICLE_ID) ?: 0L }
    private val isOnboarding: Boolean by lazy { arguments?.getBoolean(ARG_ONBOARDING) ?: false }

    private val viewModel: VehicleFormViewModel by viewModels {
        viewModelFactory { VehicleFormViewModel(appContainer, navArgVehicleId) }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val destDir = File(requireContext().filesDir, "photos").apply { mkdirs() }
        val destFile = File(destDir, "vehicle_${UUID.randomUUID()}.jpg")
        runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        }.onSuccess {
            isDirty = true
            binding.photoPicker.setImageURI(android.net.Uri.fromFile(destFile))
            viewModel.update { it.copy(photoPath = destFile.absolutePath) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehicleFormBinding.bind(inflater.inflate(R.layout.fragment_vehicle_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fuelLabels = FuelType.values().map { getString(EnumLabels.of(it)) }
        binding.fuelTypeDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, fuelLabels))
        binding.fuelTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            isDirty = true
            viewModel.update { it.copy(fuelType = FuelType.values()[position]) }
        }

        val transmissionLabels = TransmissionType.values().map { getString(EnumLabels.of(it)) }
        binding.transmissionDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, transmissionLabels))
        binding.transmissionDropdown.setOnItemClickListener { _, _, position, _ ->
            isDirty = true
            viewModel.update { it.copy(transmission = TransmissionType.values()[position]) }
        }

        binding.photoPicker.setOnClickListener { pickImage.launch("image/*") }

        binding.purchaseDateButton.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().build()
            picker.addOnPositiveButtonClickListener { selection ->
                isDirty = true
                viewModel.update { it.copy(purchaseDateMillis = selection) }
                binding.purchaseDateButton.text = com.rovena.garage.utils.Formatters.date(requireContext(), selection)
            }
            picker.show(childFragmentManager, "purchase_date")
        }

        binding.primarySwitch.setOnCheckedChangeListener { _, checked ->
            if (!isBinding) isDirty = true
            viewModel.update { it.copy(isPrimary = checked) }
        }

        wireTextInput(binding.makeInput) { text -> viewModel.update { it.copy(make = text) } }
        wireTextInput(binding.modelInput) { text -> viewModel.update { it.copy(model = text) } }
        wireTextInput(binding.yearInput) { text -> viewModel.update { it.copy(year = text) } }
        wireTextInput(binding.trimInput) { text -> viewModel.update { it.copy(trim = text) } }
        wireTextInput(binding.engineSizeInput) { text -> viewModel.update { it.copy(engineSizeLiters = text) } }
        wireTextInput(binding.mileageInput) { text -> viewModel.update { it.copy(mileage = text) } }
        wireTextInput(binding.vinInput) { text -> viewModel.update { it.copy(vin = text) } }
        wireTextInput(binding.plateInput) { text -> viewModel.update { it.copy(licensePlate = text) } }
        wireTextInput(binding.colorInput) { text -> viewModel.update { it.copy(color = text) } }
        wireTextInput(binding.purchasePriceInput) { text -> viewModel.update { it.copy(purchasePrice = text) } }
        wireTextInput(binding.estimatedValueInput) { text -> viewModel.update { it.copy(estimatedValue = text) } }
        wireTextInput(binding.notesInput) { text -> viewModel.update { it.copy(notes = text) } }

        binding.saveButton.setOnDebouncedClickListener { viewModel.save() }
        guardUnsavedChanges { isDirty }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private var isBinding = false
    private var isDirty = false

    private fun render(state: VehicleFormState) {
        if (state.isLoading) return

        isBinding = true
        if (binding.makeInput.text?.toString() != state.make) binding.makeInput.setText(state.make)
        if (binding.modelInput.text?.toString() != state.model) binding.modelInput.setText(state.model)
        if (binding.yearInput.text?.toString() != state.year) binding.yearInput.setText(state.year)
        if (binding.trimInput.text?.toString() != state.trim) binding.trimInput.setText(state.trim)
        if (binding.engineSizeInput.text?.toString() != state.engineSizeLiters) binding.engineSizeInput.setText(state.engineSizeLiters)
        if (binding.mileageInput.text?.toString() != state.mileage) binding.mileageInput.setText(state.mileage)
        if (binding.vinInput.text?.toString() != state.vin) binding.vinInput.setText(state.vin)
        if (binding.plateInput.text?.toString() != state.licensePlate) binding.plateInput.setText(state.licensePlate)
        if (binding.colorInput.text?.toString() != state.color) binding.colorInput.setText(state.color)
        if (binding.purchasePriceInput.text?.toString() != state.purchasePrice) binding.purchasePriceInput.setText(state.purchasePrice)
        if (binding.estimatedValueInput.text?.toString() != state.estimatedValue) binding.estimatedValueInput.setText(state.estimatedValue)
        if (binding.notesInput.text?.toString() != state.notes) binding.notesInput.setText(state.notes)
        binding.fuelTypeDropdown.setText(getString(EnumLabels.of(state.fuelType)), false)
        binding.transmissionDropdown.setText(getString(EnumLabels.of(state.transmission)), false)
        binding.primarySwitch.isChecked = state.isPrimary
        state.purchaseDateMillis?.let {
            binding.purchaseDateButton.text = com.rovena.garage.utils.Formatters.date(requireContext(), it)
        }
        state.photoPath?.let { path ->
            binding.photoPicker.setImageURI(android.net.Uri.fromFile(File(path)))
        }
        isBinding = false

        binding.makeLayout.error = state.errors["make"]?.let { getString(it) }
        binding.modelLayout.error = state.errors["model"]?.let { getString(it) }
        binding.yearLayout.error = state.errors["year"]?.let { getString(it) }
        val mileageWarningText = state.mileageWarning?.let { com.rovena.garage.utils.Formatters.mileageWarningText(requireContext(), it) }
        binding.mileageWarning.text = mileageWarningText
        binding.mileageWarning.visibility = if (mileageWarningText != null) View.VISIBLE else View.GONE
        binding.mileageLayout.error = state.errors["mileage"]?.let { getString(it) }

        if (state.isSaved) {
            isDirty = false
            setFragmentResult(RESULT_KEY, Bundle().apply { putLong(RESULT_VEHICLE_ID, state.savedVehicleId) })
            if (!isOnboarding) {
                findNavController().popBackStack()
            }
        }
    }

    private fun wireTextInput(input: com.google.android.material.textfield.TextInputEditText, onChanged: (String) -> Unit) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
