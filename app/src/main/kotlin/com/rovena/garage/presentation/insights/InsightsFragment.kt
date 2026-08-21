package com.rovena.garage.presentation.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentInsightsBinding
import com.rovena.garage.databinding.ItemLegendRowBinding
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.charts.BarChartView
import com.rovena.garage.presentation.common.charts.DonutChartView
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

class InsightsFragment : Fragment(R.layout.fragment_insights) {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InsightsViewModel by viewModels {
        viewModelFactory { InsightsViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private val chartColors by lazy {
        listOf(R.color.rovena_chart_1, R.color.rovena_chart_2, R.color.rovena_chart_3, R.color.rovena_chart_4, R.color.rovena_chart_5, R.color.rovena_chart_6)
            .map { ContextCompat.getColor(requireContext(), it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.bind(inflater.inflate(R.layout.fragment_insights, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emptyState.emptyTitle.text = getString(R.string.garage_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.garage_empty_message)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: InsightsUiState) {
        if (!state.hasVehicle && !state.isLoading) {
            binding.contentScroll.visibility = View.GONE
            binding.emptyState.root.visibility = View.VISIBLE
            return
        }
        if (state.isLoading) return
        binding.contentScroll.visibility = View.VISIBLE
        binding.emptyState.root.visibility = View.GONE

        bindStat(binding.statDistance, getString(R.string.insights_total_distance),
            state.totalDistanceKm?.let { Formatters.mileage(requireContext(), it, DistanceUnit.KM) } ?: getString(R.string.not_enough_data))
        bindStat(binding.statConsumption, getString(R.string.insights_avg_consumption),
            Formatters.fuelEconomy(requireContext(), state.fuelStats?.averageLitersPer100Km, FuelEconomyUnit.L_100KM))
        bindStat(binding.statCostPerKm, getString(R.string.insights_cost_per_km),
            state.expenseStats?.costPerKm?.let { Formatters.currency(requireContext(), it, AppCurrency.JOD, null) } ?: getString(R.string.not_enough_data))
        bindStat(binding.statAvgMonthly, getString(R.string.insights_avg_monthly),
            state.expenseStats?.averageMonthlyCost?.let { Formatters.currency(requireContext(), it, AppCurrency.JOD, null) } ?: getString(R.string.not_enough_data))

        binding.monthlySpendChart.bars = state.monthlySpend.map {
            BarChartView.Bar(it.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), it.total.toFloat())
        }

        val consumptionSeries = state.fuelStats?.intervals?.map { it.litersPer100Km.toFloat() } ?: emptyList()
        if (consumptionSeries.size >= 2) {
            binding.fuelTrendChart.visibility = View.VISIBLE
            binding.fuelTrendEmpty.visibility = View.GONE
            binding.fuelTrendChart.values = consumptionSeries
        } else {
            binding.fuelTrendChart.visibility = View.GONE
            binding.fuelTrendEmpty.visibility = View.VISIBLE
        }

        binding.categoryDonut.slices = state.categoryBreakdown.mapIndexed { index, slice ->
            DonutChartView.Slice(slice.total.toFloat(), chartColors[index % chartColors.size])
        }
        binding.categoryLegend.removeAllViews()
        state.categoryBreakdown.forEachIndexed { index, slice ->
            val row = ItemLegendRowBinding.inflate(LayoutInflater.from(requireContext()), binding.categoryLegend, false)
            row.legendDot.backgroundTintList = android.content.res.ColorStateList.valueOf(chartColors[index % chartColors.size])
            row.legendLabel.text = getString(EnumLabels.of(slice.category))
            row.legendValue.text = Formatters.currency(requireContext(), slice.total, AppCurrency.JOD, null)
            binding.categoryLegend.addView(row.root)
        }
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
