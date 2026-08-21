package com.rovena.garage.presentation.vehiclehub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentVehicleHubBinding
import com.rovena.garage.databinding.ItemHubSectionBinding
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors
import kotlinx.coroutines.launch
import java.io.File

class VehicleHubFragment : Fragment(R.layout.fragment_vehicle_hub) {

    private var _binding: FragmentVehicleHubBinding? = null
    private val binding get() = _binding!!

    private val argVehicleId: Long? by lazy { arguments?.getLong("vehicleId", 0L)?.takeIf { it != 0L } }

    private val viewModel: VehicleHubViewModel by viewModels {
        viewModelFactory { VehicleHubViewModel(appContainer, argVehicleId) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehicleHubBinding.bind(inflater.inflate(R.layout.fragment_vehicle_hub, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emptyState.emptyTitle.text = getString(R.string.garage_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.garage_empty_message)
        binding.emptyState.emptyAction.text = getString(R.string.add_vehicle)
        binding.emptyState.emptyAction.visibility = View.VISIBLE
        binding.emptyState.emptyAction.setOnClickListener {
            findNavController().navigate(R.id.vehicleFormFragment, bundleOf("vehicleId" to 0L))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: VehicleHubUiState) {
        val vehicle = state.vehicle
        if (vehicle == null && !state.isLoading) {
            binding.contentScroll.visibility = View.GONE
            binding.emptyState.root.visibility = View.VISIBLE
            return
        }
        if (vehicle == null) return
        binding.contentScroll.visibility = View.VISIBLE
        binding.emptyState.root.visibility = View.GONE

        binding.vehicleName.text = "${vehicle.make} ${vehicle.model}"
        binding.vehicleMeta.text = listOf(
            vehicle.year.toString(),
            getString(EnumLabels.of(vehicle.fuelType)),
            getString(EnumLabels.of(vehicle.transmission)),
            Formatters.mileage(requireContext(), vehicle.currentMileageKm, DistanceUnit.KM)
        ).joinToString(" · ")
        vehicle.photoPath?.let { binding.vehicleImage.load(File(it)) { crossfade(true) } }

        val score = state.healthScore
        binding.healthRing.progress = score ?: 0
        binding.healthScoreText.text = score?.toString() ?: "--"
        val color = androidx.core.content.ContextCompat.getColor(requireContext(), StatusColors.of(state.healthStatus))
        binding.healthRing.ringColor = color
        binding.healthStatusText.text = getString(EnumLabels.of(state.healthStatus))
        binding.healthStatusText.setTextColor(color)

        bindSection(binding.sectionMaintenance, R.drawable.ic_service, getString(R.string.hub_section_maintenance),
            getString(R.string.hub_records_count, state.maintenanceCount)) {
            findNavController().navigate(R.id.maintenanceListFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionFuel, R.drawable.ic_fuel, getString(R.string.hub_section_fuel),
            getString(R.string.hub_records_count, state.fuelCount)) {
            findNavController().navigate(R.id.fuelListFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionExpenses, R.drawable.ic_expense, getString(R.string.hub_section_expenses),
            getString(R.string.hub_records_count, state.expenseCount)) {
            findNavController().navigate(R.id.expenseListFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionDocuments, R.drawable.ic_document, getString(R.string.hub_section_documents),
            if (state.hasExpiredDocument) getString(R.string.hub_documents_expired, 1) else getString(R.string.hub_records_count, state.documentCount)) {
            findNavController().navigate(R.id.documentListFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionInspection, R.drawable.ic_inspection, getString(R.string.hub_section_inspection),
            getString(R.string.hub_view_score)) {
            findNavController().navigate(R.id.inspectionListFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionInsights, R.drawable.ic_insights, getString(R.string.hub_section_insights), "") {
            findNavController().navigate(R.id.insightsFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionTimeline, R.drawable.ic_more, getString(R.string.hub_section_timeline), "") {
            findNavController().navigate(R.id.timelineFragment, bundleOf("vehicleId" to vehicle.id))
        }
        bindSection(binding.sectionReminders, R.drawable.ic_reminder, getString(R.string.hub_section_reminders),
            getString(R.string.hub_active_reminders, state.reminderCount)) {
            findNavController().navigate(R.id.reminderListFragment, bundleOf("vehicleId" to vehicle.id))
        }
    }

    private fun bindSection(section: ItemHubSectionBinding, icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
        section.hubSectionIcon.setImageResource(icon)
        section.hubSectionTitle.text = title
        section.hubSectionSubtitle.text = subtitle
        section.hubSectionRoot.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
