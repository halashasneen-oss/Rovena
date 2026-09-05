package com.rovena.garage.data

import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class UpcomingKind { MAINTENANCE, DOCUMENT }
enum class UpcomingUrgency { OVERDUE, DUE_SOON, LATER }

data class UpcomingItem(
    val kind: UpcomingKind,
    val title: String,
    val urgency: UpcomingUrgency,
    val dueMileage: Long? = null,
    val dueAt: Long? = null,
    val remainingKm: Long? = null,
    val remainingDays: Long? = null
)

data class CostBreakdown(
    val fuel: Double,
    val maintenance: Double,
    val other: Double
) {
    val total: Double get() = fuel + maintenance + other
}

data class MonthlyCostInsights(
    val current: CostBreakdown,
    val previous: CostBreakdown,
    val changePercent: Double?,
    val distanceKm: Long?,
    val costPer100Km: Double?,
    val topOtherCategories: List<Pair<String, Double>>
)

object DashboardAnalytics {
    private const val SOON_DISTANCE_KM = 1_000L
    private val SOON_TIME_MS = TimeUnit.DAYS.toMillis(30)

    fun upcoming(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceEntity>,
        documents: List<DocumentEntity>,
        now: Long = System.currentTimeMillis()
    ): List<UpcomingItem> {
        val maintenanceItems = VehicleHealthEngine.latestMaintenanceSchedules(maintenance)
            .filter { it.nextDueMileage != null || it.nextDueAt != null }
            .map { record ->
                val remainingKm = record.nextDueMileage?.minus(vehicle.mileage)
                val remainingDays = record.nextDueAt?.let { TimeUnit.MILLISECONDS.toDays(it - now) }
                val overdue = (remainingKm != null && remainingKm <= 0L) ||
                    (record.nextDueAt != null && record.nextDueAt <= now)
                val dueSoon = !overdue && (
                    (remainingKm != null && remainingKm <= SOON_DISTANCE_KM) ||
                        (record.nextDueAt != null && record.nextDueAt - now <= SOON_TIME_MS)
                    )
                UpcomingItem(
                    kind = UpcomingKind.MAINTENANCE,
                    title = record.serviceType,
                    urgency = when {
                        overdue -> UpcomingUrgency.OVERDUE
                        dueSoon -> UpcomingUrgency.DUE_SOON
                        else -> UpcomingUrgency.LATER
                    },
                    dueMileage = record.nextDueMileage,
                    dueAt = record.nextDueAt,
                    remainingKm = remainingKm,
                    remainingDays = remainingDays
                )
            }

        val documentItems = VehicleHealthEngine.latestDocuments(documents)
            .filter { it.expiryAt != null }
            .map { record ->
                val dueAt = requireNotNull(record.expiryAt)
                val remainingDays = TimeUnit.MILLISECONDS.toDays(dueAt - now)
                val overdue = dueAt <= now
                val dueSoon = !overdue && dueAt - now <= SOON_TIME_MS
                UpcomingItem(
                    kind = UpcomingKind.DOCUMENT,
                    title = record.title,
                    urgency = when {
                        overdue -> UpcomingUrgency.OVERDUE
                        dueSoon -> UpcomingUrgency.DUE_SOON
                        else -> UpcomingUrgency.LATER
                    },
                    dueAt = dueAt,
                    remainingDays = remainingDays
                )
            }

        return (maintenanceItems + documentItems)
            .sortedWith(
                compareBy<UpcomingItem> { urgencyRank(it.urgency) }
                    .thenBy { distanceRank(it) }
                    .thenBy { it.title.lowercase() }
            )
    }

    fun monthlyCosts(
        maintenance: List<MaintenanceEntity>,
        fuel: List<FuelEntryEntity>,
        expenses: List<ExpenseEntity>,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlyCostInsights {
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(now).atZone(zoneId))
        val previousMonth = currentMonth.minusMonths(1)
        val currentStart = currentMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val previousStart = previousMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val current = breakdown(maintenance, fuel, expenses, currentStart, nextStart)
        val previous = breakdown(maintenance, fuel, expenses, previousStart, currentStart)
        val change = when {
            previous.total <= 0.0 && current.total <= 0.0 -> 0.0
            previous.total <= 0.0 -> null
            else -> ((current.total - previous.total) / previous.total) * 100.0
        }

        val currentFuel = fuel.filter { it.filledAt in currentStart until nextStart }.sortedBy { it.mileage }
        val currentMaintenance = maintenance.filter { it.performedAt in currentStart until nextStart }
        val distanceCandidates = buildList {
            addAll(currentFuel.map { it.mileage })
            addAll(currentMaintenance.map { it.mileage })
        }.filter { it >= 0L }
        val distance = if (distanceCandidates.size >= 2) {
            (distanceCandidates.maxOrNull()!! - distanceCandidates.minOrNull()!!).takeIf { it > 0L }
        } else null
        val costPer100 = distance?.let { current.total / it.toDouble() * 100.0 }

        val categories = expenses
            .filter { it.spentAt in currentStart until nextStart }
            .groupBy { it.category.trim().ifBlank { "Other" } }
            .mapValues { (_, rows) -> rows.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        return MonthlyCostInsights(
            current = current,
            previous = previous,
            changePercent = change,
            distanceKm = distance,
            costPer100Km = costPer100,
            topOtherCategories = categories
        )
    }

    private fun breakdown(
        maintenance: List<MaintenanceEntity>,
        fuel: List<FuelEntryEntity>,
        expenses: List<ExpenseEntity>,
        start: Long,
        end: Long
    ) = CostBreakdown(
        fuel = fuel.filter { it.filledAt in start until end }.sumOf { it.totalCost },
        maintenance = maintenance.filter { it.performedAt in start until end }.sumOf { it.cost },
        other = expenses.filter { it.spentAt in start until end }.sumOf { it.amount }
    )

    private fun urgencyRank(urgency: UpcomingUrgency): Int = when (urgency) {
        UpcomingUrgency.OVERDUE -> 0
        UpcomingUrgency.DUE_SOON -> 1
        UpcomingUrgency.LATER -> 2
    }

    private fun distanceRank(item: UpcomingItem): Long {
        val km = item.remainingKm?.coerceAtLeast(0L) ?: Long.MAX_VALUE / 4
        val days = item.remainingDays?.coerceAtLeast(0L)?.times(50L) ?: Long.MAX_VALUE / 4
        return minOf(km, days)
    }
}
