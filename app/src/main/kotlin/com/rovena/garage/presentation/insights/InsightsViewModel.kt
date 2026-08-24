package com.rovena.garage.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.usecase.CurrencyAggregator
import com.rovena.garage.domain.usecase.ExpenseAggregator
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import com.rovena.garage.domain.usecase.VehicleInsightGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthlySpend(val month: YearMonth, val total: Double)
data class CategorySlice(val category: ExpenseCategory, val total: Double)

data class InsightsUiState(
    val vehicleId: Long? = null,
    val hasVehicle: Boolean = false,
    val fuelStats: FuelStatsCalculator.FuelStats? = null,
    val expenseStats: ExpenseAggregator.ExpenseStats? = null,
    val monthlySpend: List<MonthlySpend> = emptyList(),
    /** Maintenance-only cost per month, isolated from [monthlySpend]'s fuel+maintenance+expense combined total, so a maintenance cost trend doesn't get masked by fuel price swings. */
    val maintenanceMonthlySpend: List<MonthlySpend> = emptyList(),
    val categoryBreakdown: List<CategorySlice> = emptyList(),
    val totalDistanceKm: Int? = null,
    /** Fuel + maintenance + expenses combined (spec: Ownership Cost Analytics), in [displayCurrencyCode] - never just the standalone Expenses total. */
    val totalOwnershipCost: Double = 0.0,
    /** [totalOwnershipCost] divided by [totalDistanceKm] - the vehicle's true cost/km, not just its expense-category cost/km. */
    val ownershipCostPerKm: Double? = null,
    val maintenanceCount: Int = 0,
    /** Local, rules-based observations from [VehicleInsightGenerator] - e.g. a fuel-economy or maintenance-cost trend worth calling out. */
    val insights: List<VehicleInsightGenerator.Insight> = emptyList(),
    /**
     * Currency the numeric [monthlySpend]/[maintenanceMonthlySpend] chart values and
     * [categoryBreakdown]/[expenseStats] totals are expressed in (spec: charts must never
     * mix currencies) - the vehicle's most common recorded currency. Null only when there's
     * no financial history at all yet.
     */
    val displayCurrencyCode: String? = null,
    /** True when this vehicle has financial records in more than one currency - some of them are excluded from the single-currency chart/breakdown above rather than silently combined. */
    val hasMixedCurrencies: Boolean = false,
    val isLoading: Boolean = true
)

class InsightsViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(InsightsUiState(isLoading = false))
        combine(
            container.vehicleRepository.observeById(vehicleId),
            container.fuelRepository.observeByVehicle(vehicleId),
            container.expenseRepository.observeByVehicle(vehicleId),
            container.maintenanceRepository.observeByVehicle(vehicleId)
        ) { vehicle, fuel, expenses, maintenance ->
            if (vehicle == null) return@combine InsightsUiState(hasVehicle = false, isLoading = false)

            val fuelEntries = fuel.sortedBy { it.mileageKm }.map {
                FuelStatsCalculator.FuelEntry(
                    date = Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                    odometerKm = it.mileageKm, liters = it.liters, totalCost = it.totalCost, isFullTank = it.isFullTank,
                    currencyCode = it.currencyCode ?: "JOD"
                )
            }
            val fuelStats = FuelStatsCalculator.compute(fuelEntries)
            val totalDistance = fuelStats.totalDistanceKm ?: run {
                val mileages = (fuel.map { it.mileageKm } + maintenance.map { it.mileageKm })
                if (mileages.size >= 2) mileages.max() - mileages.min() else null
            }

            // Charts render one numeric value per bar/slice, so every money-bearing insight
            // below is restricted to the vehicle's single most common recorded currency
            // (spec: never sum across currencies, never mix currencies in one chart) -
            // records in any other currency are excluded from these totals rather than
            // silently combined, and hasMixedCurrencies tells the Fragment to disclose that.
            val allCurrencyCodes = fuel.map { it.currencyCode ?: "JOD" } + maintenance.map { it.currencyCode ?: "JOD" } + expenses.map { it.currencyCode ?: "JOD" }
            val displayCurrency = allCurrencyCodes.groupingBy { it }.eachCount().entries.maxByOrNull { it.value }?.key
            val hasMixedCurrencies = allCurrencyCodes.distinct().size > 1
            val fuelInCurrency = fuel.filter { (it.currencyCode ?: "JOD") == displayCurrency }
            val maintenanceInCurrency = maintenance.filter { (it.currencyCode ?: "JOD") == displayCurrency }
            val expensesInCurrency = expenses.filter { (it.currencyCode ?: "JOD") == displayCurrency }

            val expenseEntries = expensesInCurrency.map {
                ExpenseAggregator.ExpenseEntry(
                    date = Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                    amount = it.amount, category = it.category
                )
            }
            val expenseStats = ExpenseAggregator.compute(expenseEntries, totalDistance)

            // Combined monthly spend (fuel + maintenance + expenses) for the last 6 months.
            val now = YearMonth.now()
            val months = (5 downTo 0).map { now.minusMonths(it.toLong()) }
            val monthlySpend = months.map { month ->
                val fuelTotal = fuelInCurrency.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()) == month }.sumOf { it.totalCost }
                val maintTotal = maintenanceInCurrency.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()) == month }.sumOf { it.cost ?: 0.0 }
                val expTotal = expensesInCurrency.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()) == month }.sumOf { it.amount }
                MonthlySpend(month, fuelTotal + maintTotal + expTotal)
            }
            val maintenanceMonthlySpend = months.map { month ->
                val maintTotal = maintenanceInCurrency.filter { YearMonth.from(Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()) == month }.sumOf { it.cost ?: 0.0 }
                MonthlySpend(month, maintTotal)
            }

            val categoryBreakdown = expenseStats.totalByCategory.entries
                .sortedByDescending { it.value }
                .take(6)
                .map { CategorySlice(it.key, it.value) }

            // Total ownership cost (spec: Ownership Cost Analytics) - fuel + maintenance +
            // expenses combined, all already restricted to displayCurrency above, so this
            // never blindly sums different currencies either.
            val totalOwnershipCost = fuelInCurrency.sumOf { it.totalCost } +
                maintenanceInCurrency.sumOf { it.cost ?: 0.0 } +
                expensesInCurrency.sumOf { it.amount }
            val ownershipCostPerKm = if (totalDistance != null && totalDistance > 0) totalOwnershipCost / totalDistance else null

            val insights = VehicleInsightGenerator.generate(
                litersPer100KmChronological = fuelStats.intervals.map { it.litersPer100Km },
                monthlyMaintenanceCostChronological = maintenanceMonthlySpend.map { it.total }
            )

            InsightsUiState(
                vehicleId = vehicle.id, hasVehicle = true, fuelStats = fuelStats, expenseStats = expenseStats,
                monthlySpend = monthlySpend, maintenanceMonthlySpend = maintenanceMonthlySpend,
                categoryBreakdown = categoryBreakdown, totalDistanceKm = totalDistance,
                totalOwnershipCost = totalOwnershipCost, ownershipCostPerKm = ownershipCostPerKm,
                maintenanceCount = maintenance.size, insights = insights,
                displayCurrencyCode = displayCurrency, hasMixedCurrencies = hasMixedCurrencies, isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())
}
