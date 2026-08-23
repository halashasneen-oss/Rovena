package com.rovena.garage.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.usecase.DueStatusCalculator
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import com.rovena.garage.domain.usecase.HealthScoreCalculator
import com.rovena.garage.domain.usecase.MileageIntelligenceCalculator
import com.rovena.garage.domain.usecase.PriorityEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class UpcomingTaskUi(
    val title: String,
    val status: DueStatus,
    val remainingKm: Int?,
    val remainingDays: Long?,
    /** Estimated calendar date (epoch millis) this mileage-based task is expected to become due, based on the vehicle's own logged driving pace. Null when there's not enough history, or the task is already date-based (remainingDays is set) and doesn't need an estimate. */
    val estimatedDateMillis: Long? = null
)

data class DashboardUiState(
    val hasAnyVehicle: Boolean = true,
    val vehicle: VehicleEntity? = null,
    val healthScore: Int? = null,
    val healthStatus: HealthStatus? = null,
    val nextService: UpcomingTaskUi? = null,
    val fuelAvgL100Km: Double? = null,
    val monthlyCost: Double = 0.0,
    /** Distance driven in the last 7 days, from the vehicle's own logged odometer readings (fuel fill-ups + maintenance records) - null when fewer than 2 readings fall in that window, rather than a fabricated number (spec: Daily/Weekly in-app summary). */
    val weeklyDistanceKm: Int? = null,
    val weeklyCost: Double = 0.0,
    val upcomingTasks: List<UpcomingTaskUi> = emptyList(),
    val vehicleStatus: PriorityEngine.VehicleStatus = PriorityEngine.VehicleStatus.HEALTHY,
    val recentActivity: List<TimelineEventEntity> = emptyList(),
    val distanceUnit: com.rovena.garage.domain.model.DistanceUnit = com.rovena.garage.domain.model.DistanceUnit.KM,
    val fuelEconomyUnit: com.rovena.garage.domain.model.FuelEconomyUnit = com.rovena.garage.domain.model.FuelEconomyUnit.L_100KM,
    val currency: com.rovena.garage.domain.model.AppCurrency = com.rovena.garage.domain.model.AppCurrency.JOD,
    val customCurrencyCode: String? = null,
    val isLoading: Boolean = true
)

private data class RecordSet1(
    val vehicle: VehicleEntity?,
    val maintenance: List<MaintenanceRecordEntity>,
    val fuel: List<FuelRecordEntity>,
    val expenses: List<ExpenseEntity>,
    val reminders: List<ReminderEntity>
)

private data class RecordSet2(
    val timeline: List<TimelineEventEntity>,
    val documents: List<DocumentEntity>,
    val settings: com.rovena.garage.data.local.entities.AppSettingsEntity,
    val conditionScores: com.rovena.garage.data.repository.InspectionConditionScores,
    val parts: List<com.rovena.garage.data.local.entities.PartEntity>
)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = container.userPreferences.currentVehicleId
        .distinctUntilChanged()
        .flatMapLatest { savedId ->
            container.vehicleRepository.observeAll().flatMapLatest allVehicles@{ vehicles ->
                if (vehicles.isEmpty()) {
                    return@allVehicles flowOf(DashboardUiState(hasAnyVehicle = false, isLoading = false))
                }
                val vehicle = vehicles.find { it.id == savedId } ?: vehicles.find { it.isPrimary } ?: vehicles.first()

                val set1 = combine(
                    container.vehicleRepository.observeById(vehicle.id),
                    container.maintenanceRepository.observeByVehicle(vehicle.id),
                    container.fuelRepository.observeByVehicle(vehicle.id),
                    container.expenseRepository.observeByVehicle(vehicle.id),
                    container.reminderRepository.observeActive(vehicle.id)
                ) { v, maintenance, fuel, expenses, reminders ->
                    RecordSet1(v, maintenance, fuel, expenses, reminders)
                }
                val set2 = combine(
                    container.timelineRepository.observeRecent(vehicle.id, 6),
                    container.documentRepository.observeByVehicle(vehicle.id),
                    container.settingsRepository.observe(),
                    container.inspectionRepository.observeLatestConditionScores(vehicle.id),
                    container.partRepository.observeByVehicle(vehicle.id)
                ) { timeline, documents, settings, conditionScores, parts -> RecordSet2(timeline, documents, settings, conditionScores, parts) }

                combine(set1, set2) { s1, s2 ->
                    buildState(s1.vehicle ?: vehicle, s1.maintenance, s1.fuel, s1.expenses, s1.reminders, s2.timeline, s2.documents, s2.settings, s2.conditionScores, s2.parts)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun buildState(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceRecordEntity>,
        fuel: List<FuelRecordEntity>,
        expenses: List<ExpenseEntity>,
        reminders: List<ReminderEntity>,
        timeline: List<TimelineEventEntity>,
        documents: List<DocumentEntity>,
        settings: com.rovena.garage.data.local.entities.AppSettingsEntity,
        conditionScores: com.rovena.garage.data.repository.InspectionConditionScores,
        parts: List<com.rovena.garage.data.local.entities.PartEntity>
    ): DashboardUiState {
        val today = LocalDate.now()
        val nowMillis = System.currentTimeMillis()

        val overdue = maintenance.count { record ->
            (record.nextDueMileageKm != null && record.nextDueMileageKm <= vehicle.currentMileageKm) ||
                (record.nextDueDateMillis != null && record.nextDueDateMillis <= nowMillis)
        }
        val tracked = maintenance.count { it.nextDueMileageKm != null || it.nextDueDateMillis != null }
        val lastMaintenanceDate = maintenance.maxOfOrNull { it.dateMillis }
        val daysSinceLastMaintenance = lastMaintenanceDate?.let { ((nowMillis - it) / 86_400_000L).toInt() }
        val hasExpiredDoc = documents.any { it.expiryDateMillis != null && it.expiryDateMillis < nowMillis }
        val hasOilRecord = maintenance.any { it.category == MaintenanceCategory.ENGINE_OIL }

        val healthInputs = HealthScoreCalculator.VehicleHealthInputs(
            daysSinceLastMaintenance = daysSinceLastMaintenance,
            overdueMaintenanceCount = if (tracked > 0) overdue else null,
            totalActiveMaintenanceItems = if (tracked > 0) tracked else null,
            brakesConditionScore = conditionScores.brakes,
            tiresConditionScore = conditionScores.tires,
            batteryConditionScore = conditionScores.battery,
            fluidsConditionScore = conditionScores.fluids,
            engineServiceUpToDate = if (!hasOilRecord) null else overdue == 0,
            transmissionServiceUpToDate = null,
            hasExpiredDocument = if (documents.isNotEmpty()) hasExpiredDoc else null,
            hasAnyTrackedDocument = documents.isNotEmpty()
        )
        val health = HealthScoreCalculator.fromVehicleInputs(healthInputs)

        // Driving pace from the vehicle's own logged odometer readings (fuel fill-ups +
        // maintenance records), used to project a labeled estimated date for mileage-only
        // due items that have no explicit date of their own (spec: Mileage Intelligence).
        val odometerReadings = fuel.map { MileageIntelligenceCalculator.OdometerReading(it.dateMillis.toLocalDate(), it.mileageKm) } +
            maintenance.map { MileageIntelligenceCalculator.OdometerReading(it.dateMillis.toLocalDate(), it.mileageKm) }
        val averageKmPerDay = MileageIntelligenceCalculator.averageKmPerDay(odometerReadings)

        fun estimatedDateMillisFor(remainingDays: Long?, estimatedDueDate: LocalDate?): Long? {
            // Only surface an estimate for tasks that don't already have a real date of
            // their own - a date-based task's estimatedDueDate just echoes its known date.
            if (remainingDays != null || estimatedDueDate == null) return null
            return estimatedDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val nextServiceEval = maintenance
            .filter { it.nextDueMileageKm != null || it.nextDueDateMillis != null }
            .mapNotNull { record ->
                DueStatusCalculator.evaluate(
                    currentMileageKm = vehicle.currentMileageKm,
                    today = today,
                    dueMileageKm = record.nextDueMileageKm,
                    dueDate = record.nextDueDateMillis?.toLocalDate(),
                    averageKmPerDay = averageKmPerDay
                )?.let { record to it }
            }
            .minByOrNull { (_, eval) -> eval.remainingKm?.toLong() ?: eval.remainingDays ?: Long.MAX_VALUE }

        val nextService = nextServiceEval?.let { (record, eval) ->
            UpcomingTaskUi(
                record.category.name, eval.status, eval.remainingKm, eval.remainingDays,
                estimatedDateMillisFor(eval.remainingDays, eval.estimatedDueDate)
            )
        }

        // "Needs Your Attention" (spec: unified priority engine) - every source of
        // something actually needing the user's attention (overdue/due-soon maintenance,
        // expiring/expired documents, due/overdue reminders) evaluated the same way and
        // combined into one ranked list, rather than three separate lists the user would
        // have to cross-reference themselves. PriorityEngine drops anything not yet urgent
        // (UPCOMING) so this never becomes a dump of every tracked item.
        data class AttentionCandidate(val attentionItem: PriorityEngine.AttentionItem, val estimatedDateMillis: Long?)

        val maintenanceCandidates = maintenance
            .filter { it.nextDueMileageKm != null || it.nextDueDateMillis != null }
            .mapNotNull { record ->
                val eval = DueStatusCalculator.evaluate(
                    currentMileageKm = vehicle.currentMileageKm, today = today,
                    dueMileageKm = record.nextDueMileageKm, dueDate = record.nextDueDateMillis?.toLocalDate(),
                    averageKmPerDay = averageKmPerDay
                ) ?: return@mapNotNull null
                AttentionCandidate(
                    PriorityEngine.AttentionItem(PriorityEngine.AttentionSourceType.MAINTENANCE, record.id, record.category.name, eval.status, eval.remainingKm, eval.remainingDays),
                    estimatedDateMillisFor(eval.remainingDays, eval.estimatedDueDate)
                )
            }

        val documentCandidates = documents.mapNotNull { doc ->
            val expiry = doc.expiryDateMillis ?: return@mapNotNull null
            val eval = DueStatusCalculator.evaluate(
                currentMileageKm = vehicle.currentMileageKm, today = today,
                dueMileageKm = null, dueDate = expiry.toLocalDate(),
                averageKmPerDay = averageKmPerDay
            ) ?: return@mapNotNull null
            AttentionCandidate(
                PriorityEngine.AttentionItem(PriorityEngine.AttentionSourceType.DOCUMENT, doc.id, doc.name, eval.status, eval.remainingKm, eval.remainingDays),
                estimatedDateMillisFor(eval.remainingDays, eval.estimatedDueDate)
            )
        }

        val reminderCandidates = reminders.mapNotNull { reminder ->
            val eval = DueStatusCalculator.evaluate(
                currentMileageKm = vehicle.currentMileageKm, today = today,
                dueMileageKm = reminder.dueMileageKm, dueDate = reminder.dueDateMillis?.toLocalDate(),
                averageKmPerDay = averageKmPerDay
            ) ?: return@mapNotNull null
            AttentionCandidate(
                PriorityEngine.AttentionItem(PriorityEngine.AttentionSourceType.REMINDER, reminder.id, reminder.title, eval.status, eval.remainingKm, eval.remainingDays),
                estimatedDateMillisFor(eval.remainingDays, eval.estimatedDueDate)
            )
        }

        val partWarrantyCandidates = parts.mapNotNull { part ->
            if (part.warrantyExpiryDateMillis == null && part.warrantyExpiryMileageKm == null) return@mapNotNull null
            val eval = DueStatusCalculator.evaluate(
                currentMileageKm = vehicle.currentMileageKm, today = today,
                dueMileageKm = part.warrantyExpiryMileageKm, dueDate = part.warrantyExpiryDateMillis?.toLocalDate(),
                averageKmPerDay = averageKmPerDay
            ) ?: return@mapNotNull null
            AttentionCandidate(
                PriorityEngine.AttentionItem(PriorityEngine.AttentionSourceType.PART_WARRANTY, part.id, part.name, eval.status, eval.remainingKm, eval.remainingDays),
                estimatedDateMillisFor(eval.remainingDays, eval.estimatedDueDate)
            )
        }

        val attentionCandidates = maintenanceCandidates + documentCandidates + reminderCandidates + partWarrantyCandidates
        val estimateByKey = attentionCandidates.associate { (it.attentionItem.type to it.attentionItem.sourceId) to it.estimatedDateMillis }
        val priorityResult = PriorityEngine.build(attentionCandidates.map { it.attentionItem })

        val upcomingTasks = priorityResult.items.map { attentionItem ->
            UpcomingTaskUi(
                attentionItem.title, attentionItem.status, attentionItem.remainingKm, attentionItem.remainingDays,
                estimateByKey[attentionItem.type to attentionItem.sourceId]
            )
        }

        val fuelEntries = fuel.sortedBy { it.mileageKm }.map {
            FuelStatsCalculator.FuelEntry(
                date = it.dateMillis.toLocalDate(),
                odometerKm = it.mileageKm,
                liters = it.liters,
                totalCost = it.totalCost,
                isFullTank = it.isFullTank
            )
        }
        val fuelStats = FuelStatsCalculator.compute(fuelEntries)

        val currentMonth = YearMonth.now()
        val monthlyFuel = fuel.filter { YearMonth.from(it.dateMillis.toLocalDate()) == currentMonth }.sumOf { it.totalCost }
        val monthlyMaintenance = maintenance.filter { YearMonth.from(it.dateMillis.toLocalDate()) == currentMonth }.sumOf { it.cost ?: 0.0 }
        val monthlyExpense = expenses.filter { YearMonth.from(it.dateMillis.toLocalDate()) == currentMonth }.sumOf { it.amount }

        // Daily/Weekly in-app summary (spec) - the trailing 7 days including today.
        val weekStart = today.minusDays(6)
        val weeklyOdometerReadings = odometerReadings.filter { !it.date.isBefore(weekStart) }
        val weeklyDistanceKm = if (weeklyOdometerReadings.size >= 2) {
            weeklyOdometerReadings.maxOf { it.mileageKm } - weeklyOdometerReadings.minOf { it.mileageKm }
        } else null
        val weeklyFuel = fuel.filter { !it.dateMillis.toLocalDate().isBefore(weekStart) }.sumOf { it.totalCost }
        val weeklyMaintenance = maintenance.filter { !it.dateMillis.toLocalDate().isBefore(weekStart) }.sumOf { it.cost ?: 0.0 }
        val weeklyExpense = expenses.filter { !it.dateMillis.toLocalDate().isBefore(weekStart) }.sumOf { it.amount }

        return DashboardUiState(
            hasAnyVehicle = true,
            vehicle = vehicle,
            healthScore = health.score,
            healthStatus = health.status,
            nextService = nextService,
            fuelAvgL100Km = fuelStats.averageLitersPer100Km,
            monthlyCost = monthlyFuel + monthlyMaintenance + monthlyExpense,
            weeklyDistanceKm = weeklyDistanceKm,
            weeklyCost = weeklyFuel + weeklyMaintenance + weeklyExpense,
            upcomingTasks = upcomingTasks.take(5),
            vehicleStatus = priorityResult.vehicleStatus,
            recentActivity = timeline.take(6),
            distanceUnit = settings.distanceUnit,
            fuelEconomyUnit = settings.fuelEconomyUnit,
            currency = settings.currency,
            customCurrencyCode = settings.customCurrencyCode,
            isLoading = false
        )
    }

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
