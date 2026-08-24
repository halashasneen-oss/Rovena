package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.usecase.CurrencyAggregator
import com.rovena.garage.utils.pdf.PdfBuilder
import com.rovena.garage.utils.pdf.withWesternNumerals
import java.io.File

object ExpensePdfGenerator {

    fun generate(context: Context, vehicle: VehicleEntity, records: List<ExpenseEntity>): File {
        val pdf = PdfBuilder(context)
        val context = context.withWesternNumerals()
        pdf.title(context.getString(R.string.pdf_expense_report_title))
        pdf.caption(context.getString(R.string.pdf_generated_on, Formatters.date(context, System.currentTimeMillis())))
        pdf.spacer()
        pdf.keyValueRow(context.getString(R.string.pdf_vehicle), "${vehicle.make} ${vehicle.model} (${vehicle.year})")
        pdf.keyValueRow(
            context.getString(R.string.pdf_total),
            Formatters.currencyTotal(context, CurrencyAggregator.aggregate(records.map { it.amount to (it.currencyCode ?: "JOD") })),
            valueAccent = true
        )
        pdf.spacer()

        val sorted = records.sortedByDescending { it.dateMillis }
        pdf.sectionHeader(context.getString(R.string.hub_section_expenses))
        sorted.forEach { expense ->
            val title = expense.description?.takeIf { it.isNotBlank() } ?: context.getString(EnumLabels.of(expense.category))
            pdf.bodyLine("${Formatters.date(context, expense.dateMillis)} · $title · ${Formatters.currency(context, expense.amount, expense.currencyCode)}")
            expense.vendor?.let { pdf.caption("  $it") }
        }

        pdf.spacer()
        pdf.sectionHeader(context.getString(R.string.insights_category_breakdown))
        records.groupBy { it.category }.entries.sortedByDescending { it.value.sumOf { r -> r.amount } }.forEach { (category, list) ->
            pdf.keyValueRow(
                context.getString(EnumLabels.of(category)),
                Formatters.currencyTotal(context, CurrencyAggregator.aggregate(list.map { it.amount to (it.currencyCode ?: "JOD") }))
            )
        }

        pdf.spacer()
        pdf.caption(context.getString(R.string.pdf_footer_privacy))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "expenses_${vehicle.id}_${System.currentTimeMillis()}.pdf")
        pdf.save(file)
        return file
    }
}
