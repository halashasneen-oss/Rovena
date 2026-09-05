package com.rovena.garage.data

import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class FuelTrend { IMPROVING, STABLE, WORSENING, UNKNOWN }

data class FuelInterval(
    val distanceKm: Long,
    val liters: Double,
    val fuelCost: Double,
    val kmPerLiter: Double,
    val costPerKm: Double
)

data class FuelInsights(
    val intervalCount: Int,
    val averageKmPerLiter: Double?,
    val litersPer100Km: Double?,
    val averageFuelCostPerKm: Double?,
    val averagePricePerLiter: Double?,
    val latestKmPerLiter: Double?,
    val trend: FuelTrend,
    val trendPercent: Double?
)

object FuelAnalytics {
    private const val MIN_REASONABLE_KM_PER_LITER = 0.5
    private const val MAX_REASONABLE_KM_PER_LITER = 150.0

    fun analyze(entries: List<FuelEntryEntity>): FuelInsights {
        val ordered = entries
            .filter { it.mileage >= 0L && it.liters > 0.0 && it.totalCost >= 0.0 }
            .sortedWith(compareBy<FuelEntryEntity> { it.mileage }.thenBy { it.filledAt }.thenBy { it.id })

        val intervals = ordered.zipWithNext().mapNotNull { (previous, current) ->
            val distance = current.mileage - previous.mileage
            if (distance <= 0L) return@mapNotNull null

            val efficiency = distance.toDouble() / current.liters
            if (efficiency !in MIN_REASONABLE_KM_PER_LITER..MAX_REASONABLE_KM_PER_LITER) {
                return@mapNotNull null
            }

            FuelInterval(
                distanceKm = distance,
                liters = current.liters,
                fuelCost = current.totalCost,
                kmPerLiter = efficiency,
                costPerKm = current.totalCost / distance.toDouble()
            )
        }

        if (intervals.isEmpty()) {
            return FuelInsights(0, null, null, null, null, null, FuelTrend.UNKNOWN, null)
        }

        val totalDistance = intervals.sumOf { it.distanceKm }.toDouble()
        val totalLiters = intervals.sumOf { it.liters }
        val totalCost = intervals.sumOf { it.fuelCost }
        val averageEfficiency = if (totalLiters > 0.0) totalDistance / totalLiters else null
        val litersPer100Km = averageEfficiency?.takeIf { it > 0.0 }?.let { 100.0 / it }
        val costPerKm = if (totalDistance > 0.0) totalCost / totalDistance else null
        val pricePerLiter = if (totalLiters > 0.0) totalCost / totalLiters else null

        val (trend, trendPercent) = calculateTrend(intervals)

        return FuelInsights(
            intervalCount = intervals.size,
            averageKmPerLiter = averageEfficiency,
            litersPer100Km = litersPer100Km,
            averageFuelCostPerKm = costPerKm,
            averagePricePerLiter = pricePerLiter,
            latestKmPerLiter = intervals.lastOrNull()?.kmPerLiter,
            trend = trend,
            trendPercent = trendPercent
        )
    }

    private fun calculateTrend(intervals: List<FuelInterval>): Pair<FuelTrend, Double?> {
        if (intervals.size < 4) return FuelTrend.UNKNOWN to null

        val recent = intervals.takeLast(2).map { it.kmPerLiter }.average()
        val previous = intervals.dropLast(2).takeLast(2).map { it.kmPerLiter }.average()
        if (previous <= 0.0) return FuelTrend.UNKNOWN to null

        val percent = ((recent - previous) / previous) * 100.0
        val trend = when {
            percent >= 5.0 -> FuelTrend.IMPROVING
            percent <= -5.0 -> FuelTrend.WORSENING
            else -> FuelTrend.STABLE
        }
        return trend to percent
    }
}

enum class HealthConfidence { LOW, MEDIUM, HIGH }
enum class VehicleHealthStatus { EXCELLENT, GOOD, ATTENTION, URGENT, UNKNOWN }

data class VehicleHealthSummary(
    val score: Int?,
    val status: VehicleHealthStatus,
    val confidence: HealthConfidence,
    val overdueMaintenance: Int,
    val dueSoonMaintenance: Int,
    val expiredDocuments: Int,
    val expiringDocuments: Int,
    val fuelTrend: FuelTrend,
    val evidenceCount: Int
) {
    val attentionCount: Int
        get() = overdueMaintenance + dueSoonMaintenance + expiredDocuments + expiringDocuments
}

object VehicleHealthEngine {
    private val MAINTENANCE_SOON_TIME = TimeUnit.DAYS.toMillis(14)
    private val DOCUMENT_SOON_TIME = TimeUnit.DAYS.toMillis(30)
    private const val MAINTENANCE_SOON_DISTANCE_KM = 500L

    fun evaluate(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceEntity>,
        documents: List<DocumentEntity>,
        fuel: List<FuelEntryEntity>,
        now: Long = System.currentTimeMillis()
    ): VehicleHealthSummary {
        val overdue = maintenance.count { isMaintenanceOverdue(it, vehicle.mileage, now) }
        val dueSoon = maintenance.count {
            !isMaintenanceOverdue(it, vehicle.mileage, now) && isMaintenanceDueSoon(it, vehicle.mileage, now)
        }
        val expired = documents.count { isDocumentExpired(it, now) }
        val expiring = documents.count { !isDocumentExpired(it, now) && isDocumentExpiringSoon(it, now) }
        val fuelInsights = FuelAnalytics.analyze(fuel)

        val evidenceCount = maintenance.count { it.nextDueMileage != null || it.nextDueAt != null } +
            documents.count { it.expiryAt != null } +
            fuelInsights.intervalCount

        val score = if (evidenceCount == 0) {
            null
        } else {
            var value = 100
            value -= (overdue * 22).coerceAtMost(44)
            value -= (dueSoon * 7).coerceAtMost(14)
            value -= (expired * 18).coerceAtMost(36)
            value -= (expiring * 5).coerceAtMost(10)
            if (fuelInsights.trend == FuelTrend.WORSENING && (fuelInsights.trendPercent ?: 0.0) <= -10.0) {
                value -= 10
            }
            value.coerceIn(0, 100)
        }

        val confidence = when {
            evidenceCount >= 6 -> HealthConfidence.HIGH
            evidenceCount >= 3 -> HealthConfidence.MEDIUM
            else -> HealthConfidence.LOW
        }

        val status = when {
            score == null -> VehicleHealthStatus.UNKNOWN
            score >= 90 -> VehicleHealthStatus.EXCELLENT
            score >= 75 -> VehicleHealthStatus.GOOD
            score >= 55 -> VehicleHealthStatus.ATTENTION
            else -> VehicleHealthStatus.URGENT
        }

        return VehicleHealthSummary(
            score = score,
            status = status,
            confidence = confidence,
            overdueMaintenance = overdue,
            dueSoonMaintenance = dueSoon,
            expiredDocuments = expired,
            expiringDocuments = expiring,
            fuelTrend = fuelInsights.trend,
            evidenceCount = evidenceCount
        )
    }

    fun isMaintenanceOverdue(record: MaintenanceEntity, mileage: Long, now: Long): Boolean =
        (record.nextDueMileage != null && record.nextDueMileage <= mileage) ||
            (record.nextDueAt != null && record.nextDueAt <= now)

    fun isMaintenanceDueSoon(
        record: MaintenanceEntity,
        mileage: Long,
        now: Long,
        distanceWindowKm: Long = MAINTENANCE_SOON_DISTANCE_KM,
        timeWindowMillis: Long = MAINTENANCE_SOON_TIME
    ): Boolean {
        val byMileage = record.nextDueMileage?.let { due ->
            due > mileage && due - mileage <= distanceWindowKm
        } ?: false
        val byDate = record.nextDueAt?.let { due ->
            due > now && due - now <= timeWindowMillis
        } ?: false
        return byMileage || byDate
    }

    fun isDocumentExpired(document: DocumentEntity, now: Long): Boolean =
        document.expiryAt?.let { it <= now } ?: false

    fun isDocumentExpiringSoon(
        document: DocumentEntity,
        now: Long,
        horizonMillis: Long = DOCUMENT_SOON_TIME
    ): Boolean = document.expiryAt?.let { it > now && it - now <= horizonMillis } ?: false

    fun isMeaningfulFuelDrop(insights: FuelInsights, thresholdPercent: Double = 15.0): Boolean =
        insights.trend == FuelTrend.WORSENING && abs(insights.trendPercent ?: 0.0) >= thresholdPercent
}
