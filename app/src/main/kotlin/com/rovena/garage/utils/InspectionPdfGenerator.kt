package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.presentation.inspection.InspectionFormState
import com.rovena.garage.utils.pdf.PdfBuilder
import com.rovena.garage.utils.pdf.withWesternNumerals
import java.io.File

object InspectionPdfGenerator {

    fun generate(context: Context, vehicle: VehicleEntity, state: InspectionFormState): File {
        val pdf = PdfBuilder(context)
        val context = context.withWesternNumerals()
        pdf.title(context.getString(R.string.pdf_inspection_report_title))
        pdf.caption(context.getString(R.string.pdf_generated_on, Formatters.date(context, System.currentTimeMillis())))
        pdf.spacer()

        pdf.keyValueRow(context.getString(R.string.pdf_vehicle), "${vehicle.make} ${vehicle.model} (${vehicle.year})")
        pdf.keyValueRow(context.getString(R.string.maintenance_field_date), Formatters.date(context, state.dateMillis))
        pdf.keyValueRow(context.getString(R.string.vehicle_field_mileage), Formatters.mileage(context, state.mileage.toIntOrNull() ?: vehicle.currentMileageKm, com.rovena.garage.domain.model.DistanceUnit.KM))
        vehicle.vin?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_vin), it) }
        vehicle.licensePlate?.let { pdf.keyValueRow(context.getString(R.string.vehicle_field_plate), it) }
        pdf.spacer()

        pdf.keyValueRow(context.getString(R.string.inspection_score_label), state.liveScoreResult.score?.let { "$it / 100" } ?: context.getString(R.string.not_enough_data), valueAccent = true)
        pdf.keyValueRow(context.getString(R.string.inspection_issues_found, state.liveScoreResult.problemCount), "")
        pdf.keyValueRow(context.getString(R.string.inspection_attention_items, state.liveScoreResult.attentionCount), "")
        pdf.keyValueRow(context.getString(R.string.inspection_good_items, state.liveScoreResult.goodCount), "")

        listOf(
            InspectionCategoryGroup.EXTERIOR to R.string.inspection_category_exterior,
            InspectionCategoryGroup.INTERIOR to R.string.inspection_category_interior,
            InspectionCategoryGroup.MECHANICAL to R.string.inspection_category_mechanical
        ).forEach { (group, headerRes) ->
            pdf.sectionHeader(context.getString(headerRes))
            state.items.filter { it.categoryGroup == group }.forEach { item ->
                val statusLabel = context.getString(EnumLabels.of(item.status))
                val line = context.getString(EnumLabels.of(item.itemKey)) + ": " + statusLabel +
                    (item.estimatedRepairCost.toDoubleOrNull()?.let { " (est. $it)" } ?: "")
                pdf.bodyLine(line)
                if (item.notes.isNotBlank()) pdf.caption("  " + item.notes)
            }
        }

        pdf.spacer()
        pdf.caption(context.getString(R.string.pdf_footer_privacy))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "inspection_${vehicle.id}_${System.currentTimeMillis()}.pdf")
        pdf.save(file)
        return file
    }
}
