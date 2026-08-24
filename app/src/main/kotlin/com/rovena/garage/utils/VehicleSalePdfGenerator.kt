package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.AppContainer
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.data.repository.InspectionConditionScores
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import com.rovena.garage.domain.usecase.InspectionScoreCalculator
import com.rovena.garage.utils.pdf.PdfBuilder
import com.rovena.garage.utils.pdf.withWesternNumerals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * A report meant to be handed to a prospective buyer (spec: Vehicle Sale
 * Report), distinct from [VehicleSummaryPdfGenerator]'s personal-dashboard
 * summary in what it deliberately shows and omits: full itemized service
 * history and the latest inspection's condition breakdown build buyer trust,
 * while purchase price and current estimated value - the owner's private
 * financial figures, not the buyer's business - are never included.
 */
object VehicleSalePdfGenerator {

    suspend fun generate(context: Context, container: AppContainer, vehicle: VehicleEntity): File {
        val maintenance = firstOnce(container.maintenanceRepository.observeByVehicle(vehicle.id)).sortedByDescending { it.dateMillis }
        val fuel = firstOnce(container.fuelRepository.observeByVehicle(vehicle.id))
        val documents = firstOnce(container.documentRepository.observeByVehicle(vehicle.id))
        val parts = firstOnce(container.partRepository.observeByVehicle(vehicle.id))
        val latestInspection = container.inspectionRepository.getLatest(vehicle.id)
        val latestInspectionItems = latestInspection?.let { container.inspectionRepository.getItemsOnce(it.id) } ?: emptyList()

        val itemsByKey = latestInspectionItems.associateBy { it.itemKey }
        fun conditionScore(key: InspectionItemKey) = itemsByKey[key]?.status?.let(InspectionScoreCalculator::conditionScoreFor)

        val hasExpiredDocument = documents.any { it.expiryDateMillis != null && it.expiryDateMillis < System.currentTimeMillis() }
        val conditionScores = InspectionConditionScores(
            brakes = conditionScore(InspectionItemKey.BRAKES),
            tires = conditionScore(InspectionItemKey.TIRES),
            battery = conditionScore(InspectionItemKey.BATTERY),
            fluids = conditionScore(InspectionItemKey.FLUIDS)
        )
        val health = HealthInputsBuilder.calculate(vehicle, maintenance, documents, conditionScores)

        val fuelEntries = fuel.sortedBy { it.mileageKm }.map {
            FuelStatsCalculator.FuelEntry(
                date = Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                odometerKm = it.mileageKm, liters = it.liters, totalCost = it.totalCost, isFullTank = it.isFullTank,
                currencyCode = it.currencyCode ?: "JOD"
            )
        }
        val fuelStats = FuelStatsCalculator.compute(fuelEntries)

        val pdf = PdfBuilder(context)
        val context = context.withWesternNumerals()
        pdf.title(context.getString(R.string.pdf_sale_report_title))
        pdf.caption(context.getString(R.string.pdf_generated_on, Formatters.date(context, System.currentTimeMillis())))
        pdf.spacer()

        pdf.sectionHeader("${vehicle.make} ${vehicle.model}")
        pdf.keyValueRow(context.getString(R.string.vehicle_field_year), vehicle.year.toString())
        vehicle.trim?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_trim), it) }
        pdf.keyValueRow(context.getString(R.string.vehicle_field_fuel_type), context.getString(EnumLabels.of(vehicle.fuelType)))
        pdf.keyValueRow(context.getString(R.string.vehicle_field_transmission), context.getString(EnumLabels.of(vehicle.transmission)))
        pdf.keyValueRow(context.getString(R.string.vehicle_field_mileage), Formatters.mileage(context, vehicle.currentMileageKm, DistanceUnit.KM), valueAccent = true)
        vehicle.vin?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_vin), it) }
        vehicle.licensePlate?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_plate), it) }
        vehicle.color?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_color), it) }

        pdf.sectionHeader(context.getString(R.string.dashboard_vehicle_health))
        pdf.keyValueRow(
            context.getString(R.string.inspection_score_label),
            health.score?.let { "$it / 100 (${context.getString(EnumLabels.of(health.status))})" } ?: context.getString(R.string.not_enough_data),
            valueAccent = true
        )
        if (latestInspection != null) {
            pdf.keyValueRow(context.getString(R.string.pdf_sale_last_inspection_date), Formatters.date(context, latestInspection.dateMillis))
            latestInspectionItems.filter { it.status != InspectionItemStatus.UNKNOWN }
                .sortedBy { context.getString(EnumLabels.of(it.itemKey)) }
                .forEach { item ->
                    pdf.bodyLine("${context.getString(EnumLabels.of(item.itemKey))}: ${context.getString(EnumLabels.of(item.status))}")
                }
        } else {
            pdf.caption(context.getString(R.string.pdf_sale_no_inspection))
        }

        pdf.sectionHeader(context.getString(R.string.pdf_sale_fuel_economy))
        pdf.keyValueRow(
            context.getString(R.string.insights_avg_consumption),
            Formatters.fuelEconomy(context, fuelStats.averageLitersPer100Km, FuelEconomyUnit.L_100KM)
        )

        pdf.sectionHeader(context.getString(R.string.hub_section_maintenance))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, maintenance.size), "")
        maintenance.forEach { record ->
            val costText = record.cost?.let { Formatters.currency(context, it, record.currencyCode) } ?: ""
            pdf.bodyLine("${Formatters.date(context, record.dateMillis)} · ${context.getString(EnumLabels.of(record.category))} · $costText")
            pdf.caption("  ${record.description}" + (record.workshop?.let { " · $it" } ?: "") + " · " + Formatters.mileage(context, record.mileageKm, DistanceUnit.KM))
        }

        pdf.sectionHeader(context.getString(R.string.hub_section_documents))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, documents.size), "")
        pdf.keyValueRow(context.getString(R.string.pdf_sale_documents_current), if (hasExpiredDocument) context.getString(R.string.pdf_sale_documents_expired) else context.getString(R.string.pdf_sale_documents_ok))

        pdf.sectionHeader(context.getString(R.string.hub_section_parts))
        if (parts.isEmpty()) {
            pdf.caption(context.getString(R.string.pdf_sale_no_parts))
        } else {
            parts.sortedByDescending { it.installedDateMillis }.forEach { part ->
                val hasWarranty = part.warrantyExpiryDateMillis != null || part.warrantyExpiryMileageKm != null
                val warrantyText = if (!hasWarranty) {
                    context.getString(R.string.pdf_sale_part_no_warranty)
                } else {
                    val dateStillValid = part.warrantyExpiryDateMillis?.let { it > System.currentTimeMillis() } ?: true
                    val mileageStillValid = part.warrantyExpiryMileageKm?.let { vehicle.currentMileageKm < it } ?: true
                    if (dateStillValid && mileageStillValid) context.getString(R.string.pdf_sale_part_warranty_active) else context.getString(R.string.pdf_sale_part_warranty_expired)
                }
                pdf.bodyLine("${part.name} · ${Formatters.date(context, part.installedDateMillis)} · $warrantyText")
            }
        }

        pdf.spacer()
        pdf.divider()
        pdf.sectionHeader(context.getString(R.string.pdf_sale_disclaimer_title))
        pdf.caption(context.getString(R.string.pdf_sale_disclaimer_text))

        pdf.spacer()
        pdf.caption(context.getString(R.string.pdf_footer_sale_report))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "sale_report_${vehicle.id}_${System.currentTimeMillis()}.pdf")
        pdf.save(file)
        return file
    }

    private suspend fun <T> firstOnce(flow: Flow<List<T>>): List<T> = flow.first()
}
