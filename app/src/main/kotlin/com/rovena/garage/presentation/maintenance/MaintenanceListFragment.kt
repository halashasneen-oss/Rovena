package com.rovena.garage.presentation.maintenance

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.databinding.FragmentGenericListBinding
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.usecase.ServicePlanCatalog
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.MaintenancePdfGenerator
import com.rovena.garage.utils.PdfViewerLauncher
import kotlinx.coroutines.launch

class MaintenanceListFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaintenanceListViewModel by viewModels {
        viewModelFactory { MaintenanceListViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private val adapter = MaintenanceAdapter { row ->
        findNavController().navigate(R.id.maintenanceFormFragment, bundleOf("vehicleId" to row.record.vehicleId, "recordId" to row.record.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_maintenance)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter

        binding.emptyState.emptyTitle.text = getString(R.string.maintenance_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.maintenance_empty_message)

        binding.fabAdd.setOnClickListener {
            val vehicleId = viewModel.uiState.value.vehicleId ?: return@setOnClickListener
            findNavController().navigate(R.id.maintenanceFormFragment, bundleOf("vehicleId" to vehicleId, "recordId" to 0L))
        }

        binding.pdfButton.visibility = View.VISIBLE
        binding.pdfButton.setOnClickListener { generatePdf() }

        binding.servicePlanButton.visibility = View.VISIBLE
        binding.servicePlanButton.setOnClickListener { showServicePlanChooserDialog() }

        binding.filterButton.visibility = View.VISIBLE
        binding.filterButton.setOnClickListener { showFilterDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.rows)
                    binding.screenSummary.text = Formatters.currencyTotal(requireContext(), state.totalCost)
                    val empty = !state.isLoading && state.rows.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                    binding.pdfButton.isEnabled = state.rows.isNotEmpty()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appContainer.settingsRepository.observe().collect { adapter.setDistanceUnit(it.distanceUnit) }
            }
        }
    }

    private fun showFilterDialog() {
        val categories = MaintenanceCategory.values()
        val labels = arrayOf(getString(R.string.timeline_filter_all)) + categories.map { getString(EnumLabels.of(it)) }
        val checkedIndex = viewModel.uiState.value.filter?.let { categories.indexOf(it) + 1 } ?: 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_filter)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                viewModel.setFilter(if (which == 0) null else categories[which - 1])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Spec: Service Plans / maintenance bundles. Applying a plan is still a
     * two-step, fully-visible confirmation - a chooser, then one small form
     * the user reviews before anything is saved - never a silent bulk-insert.
     */
    private fun showServicePlanChooserDialog() {
        val plans = ServicePlanCatalog.PLANS
        val labels = plans.map { getString(servicePlanLabel(it.id)) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.service_plan_dialog_title)
            .setItems(labels) { _, which -> showServicePlanFormDialog(plans[which]) }
            .show()
    }

    private fun showServicePlanFormDialog(plan: ServicePlanCatalog.ServicePlan) {
        val vehicleId = viewModel.uiState.value.vehicleId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val vehicle = appContainer.vehicleRepository.getById(vehicleId) ?: return@launch
            var dateMillis = System.currentTimeMillis()

            val dateButton = Button(requireContext())
            dateButton.text = Formatters.date(requireContext(), dateMillis)
            dateButton.setOnClickListener {
                DatePickerHelper.show(childFragmentManager, "service_plan_date", dateMillis) { picked ->
                    dateMillis = picked
                    dateButton.text = Formatters.date(requireContext(), picked)
                }
            }
            val mileageInput = EditText(requireContext()).apply {
                hint = getString(R.string.vehicle_field_mileage)
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(vehicle.currentMileageKm.toString())
            }
            val workshopInput = EditText(requireContext()).apply {
                hint = getString(R.string.maintenance_field_workshop)
            }
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (20 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                addView(dateButton)
                addView(mileageInput)
                addView(workshopInput)
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(servicePlanLabel(plan.id)))
                .setMessage(getString(R.string.service_plan_form_message, plan.categories.size))
                .setView(container)
                .setPositiveButton(R.string.action_continue) { _, _ ->
                    val mileage = mileageInput.text.toString().toIntOrNull() ?: vehicle.currentMileageKm
                    val workshop = workshopInput.text.toString().trim().ifBlank { null }
                    applyServicePlan(vehicleId, plan, dateMillis, mileage, workshop)
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private fun applyServicePlan(vehicleId: Long, plan: ServicePlanCatalog.ServicePlan, dateMillis: Long, mileageKm: Int, workshop: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            plan.categories.forEach { category ->
                appContainer.maintenanceRepository.addOrUpdate(
                    MaintenanceRecordEntity(
                        vehicleId = vehicleId, dateMillis = dateMillis, mileageKm = mileageKm, category = category,
                        description = getString(EnumLabels.of(category)), workshop = workshop
                    )
                )
            }
            android.widget.Toast.makeText(requireContext(), getString(R.string.service_plan_added_success, plan.categories.size), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun servicePlanLabel(planId: String): Int = when (planId) {
        "minor_service" -> R.string.service_plan_minor_service
        "major_service" -> R.string.service_plan_major_service
        "brake_service" -> R.string.service_plan_brake_service
        else -> R.string.service_plan_dialog_title
    }

    private fun generatePdf() {
        val vehicleId = viewModel.uiState.value.vehicleId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val vehicle = appContainer.vehicleRepository.getById(vehicleId) ?: return@launch
            val records = viewModel.uiState.value.rows.map { it.record }
            val distanceUnit = appContainer.settingsRepository.getOrDefault().distanceUnit
            val file = MaintenancePdfGenerator.generate(requireContext(), vehicle, records, distanceUnit)
            PdfViewerLauncher.open(this@MaintenanceListFragment, file)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
