package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.usecase.CurrencyAggregator
import com.rovena.garage.utils.pdf.PdfBuilder
import com.rovena.garage.utils.pdf.withWesternNumerals
import java.io.File

object MaintenancePdfGenerator {

    fun generate(context: Context, vehicle: VehicleEntity, records: List<MaintenanceRecordEntity>): File {
        val pdf = PdfBuilder(context)
        val context = context.withWesternNumerals()
        pdf.title(context.getString(R.string.pdf_maintenance_report_title))
        pdf.caption(context.getString(R.string.pdf_generated_on, Formatters.date(context, System.currentTimeMillis())))
        pdf.spacer()
        pdf.keyValueRow(context.getString(R.string.pdf_vehicle), "${vehicle.make} ${vehicle.model} (${vehicle.year})")
        pdf.keyValueRow(
            context.getString(R.string.pdf_total),
            Formatters.currencyTotal(context, CurrencyAggregator.aggregate(records.mapNotNull { r -> r.cost?.let { it to (r.currencyCode ?: "JOD") } })),
            valueAccent = true
        )
        pdf.spacer()

        val sorted = records.sortedByDescending { it.dateMillis }
        pdf.sectionHeader(context.getString(R.string.hub_section_maintenance))
        sorted.forEach { record ->
            val costText = record.cost?.let { Formatters.currency(context, it, record.currencyCode) } ?: ""
            pdf.bodyLine("${Formatters.date(context, record.dateMillis)} · ${context.getString(EnumLabels.of(record.category))} · $costText")
            pdf.caption("  ${record.description}" + (record.workshop?.let { " · $it" } ?: "") + " · " + Formatters.mileage(context, record.mileageKm, DistanceUnit.KM))
        }

        pdf.spacer()
        pdf.sectionHeader(context.getString(R.string.insights_category_breakdown))
        records.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf { r -> r.cost ?: 0.0 } }.forEach { (category, list) ->
            pdf.keyValueRow(
                context.getString(EnumLabels.of(category)),
                Formatters.currencyTotal(context, CurrencyAggregator.aggregate(list.mapNotNull { r -> r.cost?.let { it to (r.currencyCode ?: "JOD") } }))
            )
        }

        pdf.spacer()
        pdf.caption(context.getString(R.string.pdf_footer_privacy))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "maintenance_${vehicle.id}_${System.currentTimeMillis()}.pdf")
        pdf.save(file)
        return file
    }
}
