package com.rovena.garage.presentation.dashboard

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentDashboardBinding
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.presentation.common.CircularScoreView
import com.rovena.garage.presentation.common.TimelineEventAdapter
import com.rovena.garage.presentation.common.UpcomingTaskAdapter
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels { viewModelFactory { DashboardViewModel(appContainer) } }

    private val upcomingAdapter = UpcomingTaskAdapter()
    private val recentAdapter = TimelineEventAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.bind(inflater.inflate(R.layout.fragment_dashboard, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.upcomingTasksRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.upcomingTasksRecycler.adapter = upcomingAdapter
        binding.recentActivityRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recentActivityRecycler.adapter = recentAdapter

        setupQuickActions()

        binding.emptyState.root.visibility = View.GONE
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

    private fun setupQuickActions() {
        bindQuickAction(binding.quickAddFuel, R.drawable.ic_fuel, R.string.quick_action_fuel) {
            currentVehicleId()?.let { findNavController().navigate(R.id.fuelFormFragment, bundleOf("vehicleId" to it, "recordId" to 0L)) }
        }
        bindQuickAction(binding.quickAddMaintenance, R.drawable.ic_service, R.string.quick_action_maintenance) {
            currentVehicleId()?.let { findNavController().navigate(R.id.maintenanceFormFragment, bundleOf("vehicleId" to it, "recordId" to 0L)) }
        }
        bindQuickAction(binding.quickAddExpense, R.drawable.ic_expense, R.string.quick_action_expense) {
            currentVehicleId()?.let { findNavController().navigate(R.id.expenseFormFragment, bundleOf("vehicleId" to it, "recordId" to 0L)) }
        }
        bindQuickAction(binding.quickAddDocument, R.drawable.ic_document, R.string.quick_action_document) {
            currentVehicleId()?.let { findNavController().navigate(R.id.documentFormFragment, bundleOf("vehicleId" to it, "recordId" to 0L)) }
        }
        bindQuickAction(binding.quickAddInspection, R.drawable.ic_inspection, R.string.quick_action_inspection) {
            currentVehicleId()?.let { findNavController().navigate(R.id.inspectionFormFragment, bundleOf("vehicleId" to it, "recordId" to 0L)) }
        }
    }

    private fun currentVehicleId(): Long? = viewModel.uiState.value.vehicle?.id

    private fun bindQuickAction(included: com.rovena.garage.databinding.ItemQuickActionBinding, icon: Int, label: Int, onClick: () -> Unit) {
        included.quickActionIcon.setImageResource(icon)
        included.quickActionLabel.setText(label)
        included.quickActionRoot.setOnClickListener { onClick() }
    }

    private fun render(state: DashboardUiState) {
        if (!state.hasAnyVehicle) {
            binding.contentScroll.visibility = View.GONE
            binding.emptyState.root.visibility = View.VISIBLE
            return
        }
        binding.contentScroll.visibility = View.VISIBLE
        binding.emptyState.root.visibility = View.GONE

        val vehicle = state.vehicle ?: return
        binding.vehicleName.text = "${vehicle.make} ${vehicle.model}"
        binding.vehicleMileage.text = Formatters.mileage(requireContext(), vehicle.currentMileageKm, state.distanceUnit)
        recentAdapter.setDistanceUnit(state.distanceUnit)

        val score = state.healthScore
        binding.healthRing.progress = score ?: 0
        binding.healthScoreText.text = score?.let { "$it" } ?: "--"
        val status = state.healthStatus ?: HealthStatus.NOT_ENOUGH_DATA
        binding.healthStatusText.text = getString(EnumLabels.of(status))
        val color = androidx.core.content.ContextCompat.getColor(requireContext(), StatusColors.of(status))
        binding.healthRing.ringColor = color
        binding.healthStatusText.setTextColor(color)

        bindStat(binding.statNextService, getString(R.string.dashboard_next_service),
            state.nextService?.remainingKm?.let { Formatters.mileage(requireContext(), it.coerceAtLeast(0), state.distanceUnit) }
                ?: state.nextService?.remainingDays?.let { "${it}d" }
                ?: getString(R.string.not_enough_data))
        bindStat(binding.statFuel, getString(R.string.dashboard_fuel),
            Formatters.fuelEconomy(requireContext(), state.fuelAvgL100Km, state.fuelEconomyUnit))
        bindStat(binding.statMonthlyCost, getString(R.string.dashboard_monthly_cost),
            Formatters.currency(requireContext(), state.monthlyCost, state.currency, state.customCurrencyCode))
        bindStat(binding.statWeeklyDistance, getString(R.string.dashboard_weekly_distance),
            state.weeklyDistanceKm?.let { Formatters.mileage(requireContext(), it, state.distanceUnit) } ?: getString(R.string.not_enough_data))
        bindStat(binding.statWeeklySpend, getString(R.string.dashboard_weekly_spend),
            Formatters.currency(requireContext(), state.weeklyCost, state.currency, state.customCurrencyCode))

        binding.upcomingEmptyText.visibility = View.VISIBLE
        binding.upcomingEmptyText.text = if (state.upcomingTasks.isEmpty()) {
            getString(R.string.dashboard_no_upcoming_tasks)
        } else {
            getString(R.string.dashboard_attention_count, state.upcomingTasks.size)
        }
        binding.upcomingEmptyText.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), StatusColors.of(state.vehicleStatus))
        )
        binding.upcomingTasksRecycler.visibility = if (state.upcomingTasks.isEmpty()) View.GONE else View.VISIBLE
        upcomingAdapter.submitList(state.upcomingTasks)

        binding.recentActivityEmptyText.visibility = if (state.recentActivity.isEmpty()) View.VISIBLE else View.GONE
        binding.recentActivityRecycler.visibility = if (state.recentActivity.isEmpty()) View.GONE else View.VISIBLE
        recentAdapter.submitList(state.recentActivity)
    }

    private fun bindStat(included: com.rovena.garage.databinding.ItemStatCardBinding, label: String, value: String) {
        included.statLabel.text = label
        included.statValue.text = value
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
