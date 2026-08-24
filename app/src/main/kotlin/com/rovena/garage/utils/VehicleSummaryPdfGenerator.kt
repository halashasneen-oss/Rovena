package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.AppContainer
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.usecase.CurrencyAggregator
import com.rovena.garage.utils.pdf.PdfBuilder
import com.rovena.garage.utils.pdf.withWesternNumerals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

object VehicleSummaryPdfGenerator {

    suspend fun generate(context: Context, container: AppContainer, vehicle: VehicleEntity): File {
        val maintenance = firstOnce(container.maintenanceRepository.observeByVehicle(vehicle.id))
        val fuel = firstOnce(container.fuelRepository.observeByVehicle(vehicle.id))
        val expenses = firstOnce(container.expenseRepository.observeByVehicle(vehicle.id))
        val documents = firstOnce(container.documentRepository.observeByVehicle(vehicle.id))
        val reminders = firstOnce(container.reminderRepository.observeActive(vehicle.id))
        val conditionScores = container.inspectionRepository.observeLatestConditionScores(vehicle.id).first()
        val health = HealthInputsBuilder.calculate(vehicle, maintenance, documents, conditionScores)
        val distanceUnit = container.settingsRepository.getOrDefault().distanceUnit

        val pdf = PdfBuilder(context)
        val context = context.withWesternNumerals()
        pdf.title(context.getString(R.string.pdf_vehicle_summary_title))
        pdf.caption(context.getString(R.string.pdf_generated_on, Formatters.date(context, System.currentTimeMillis())))
        pdf.spacer()

        pdf.sectionHeader("${vehicle.make} ${vehicle.model}")
        pdf.keyValueRow(context.getString(R.string.vehicle_field_year), vehicle.year.toString())
        vehicle.trim?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_trim), it) }
        pdf.keyValueRow(context.getString(R.string.vehicle_field_fuel_type), context.getString(EnumLabels.of(vehicle.fuelType)))
        pdf.keyValueRow(context.getString(R.string.vehicle_field_transmission), context.getString(EnumLabels.of(vehicle.transmission)))
        pdf.keyValueRow(context.getString(R.string.vehicle_field_mileage), Formatters.mileage(context, vehicle.currentMileageKm, distanceUnit))
        vehicle.vin?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_vin), it) }
        vehicle.licensePlate?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_plate), it) }
        vehicle.color?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_color), it) }
        vehicle.purchaseDateMillis?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_purchase_date), Formatters.date(context, it)) }
        vehicle.purchasePrice?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_purchase_price), Formatters.currency(context, it, vehicle.currencyCode)) }
        vehicle.currentEstimatedValue?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_estimated_value), Formatters.currency(context, it, vehicle.currencyCode)) }

        pdf.sectionHeader(context.getString(R.string.dashboard_vehicle_health))
        pdf.keyValueRow(
            context.getString(R.string.inspection_score_label),
            health.score?.let { "$it / 100 (${context.getString(EnumLabels.of(health.status))})" } ?: context.getString(R.string.not_enough_data),
            valueAccent = true
        )

        pdf.sectionHeader(context.getString(R.string.hub_section_maintenance))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, maintenance.size), "")
        pdf.keyValueRow(
            context.getString(R.string.pdf_total),
            Formatters.currencyTotal(context, CurrencyAggregator.aggregate(maintenance.mapNotNull { r -> r.cost?.let { it to (r.currencyCode ?: "JOD") } }))
        )

        pdf.sectionHeader(context.getString(R.string.hub_section_fuel))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, fuel.size), "")
        pdf.keyValueRow(
            context.getString(R.string.pdf_total),
            Formatters.currencyTotal(context, CurrencyAggregator.aggregate(fuel.map { it.totalCost to (it.currencyCode ?: "JOD") }))
        )

        pdf.sectionHeader(context.getString(R.string.hub_section_expenses))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, expenses.size), "")
        pdf.keyValueRow(
            context.getString(R.string.pdf_total),
            Formatters.currencyTotal(context, CurrencyAggregator.aggregate(expenses.map { it.amount to (it.currencyCode ?: "JOD") }))
        )

        pdf.sectionHeader(context.getString(R.string.hub_section_documents))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, documents.size), "")

        pdf.sectionHeader(context.getString(R.string.hub_section_reminders))
        pdf.keyValueRow(context.getString(R.string.hub_records_count, reminders.size), "")

        pdf.spacer()
        pdf.caption(context.getString(R.string.pdf_footer_privacy))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "vehicle_summary_${vehicle.id}_${System.currentTimeMillis()}.pdf")
        pdf.save(file)
        return file
    }

    private suspend fun <T> firstOnce(flow: Flow<List<T>>): List<T> = flow.first()
}
