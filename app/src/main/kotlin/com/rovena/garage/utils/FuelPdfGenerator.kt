package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.domain.usecase.CurrencyAggregator
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import com.rovena.garage.utils.pdf.PdfBuilder
import java.io.File
import java.time.Instant
import java.time.ZoneId

object FuelPdfGenerator {

    fun generate(context: Context, vehicle: VehicleEntity, records: List<FuelRecordEntity>): File {
        val entries = records.sortedBy { it.mileageKm }.map {
            FuelStatsCalculator.FuelEntry(
                date = Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                odometerKm = it.mileageKm, liters = it.liters, totalCost = it.totalCost, isFullTank = it.isFullTank
            )
        }
        val stats = FuelStatsCalculator.compute(entries)

        val pdf = PdfBuilder()
        pdf.title(context.getString(R.string.pdf_fuel_report_title))
        pdf.caption(context.getString(R.string.pdf_generated_on, Formatters.date(context, System.currentTimeMillis())))
        pdf.spacer()
        pdf.keyValueRow(context.getString(R.string.pdf_vehicle), "${vehicle.make} ${vehicle.model} (${vehicle.year})")
        pdf.keyValueRow(
            context.getString(R.string.pdf_total),
            Formatters.currencyTotal(context, CurrencyAggregator.aggregate(records.map { it.totalCost to (it.currencyCode ?: "JOD") })),
            valueAccent = true
        )
        pdf.spacer()

        pdf.sectionHeader(context.getString(R.string.insights_avg_consumption))
        pdf.keyValueRow(context.getString(R.string.fuel_stats_avg, "").trim(), Formatters.fuelEconomy(context, stats.averageLitersPer100Km, FuelEconomyUnit.L_100KM))
        pdf.keyValueRow(context.getString(R.string.fuel_stats_best, "").trim(), Formatters.fuelEconomy(context, stats.bestLitersPer100Km, FuelEconomyUnit.L_100KM))
        pdf.keyValueRow(context.getString(R.string.fuel_stats_worst, "").trim(), Formatters.fuelEconomy(context, stats.worstLitersPer100Km, FuelEconomyUnit.L_100KM))
        stats.totalDistanceKm?.let { pdf.keyValueRow(context.getString(R.string.insights_total_distance), Formatters.mileage(context, it, DistanceUnit.KM)) }

        pdf.spacer()
        pdf.sectionHeader(context.getString(R.string.hub_section_fuel))
        records.sortedByDescending { it.dateMillis }.forEach { record ->
            pdf.bodyLine("${Formatters.date(context, record.dateMillis)} · ${record.liters} L · ${Formatters.currency(context, record.totalCost, record.currencyCode)}")
            pdf.caption("  " + Formatters.mileage(context, record.mileageKm, DistanceUnit.KM) + (record.station?.let { " · $it" } ?: ""))
        }

        pdf.spacer()
        pdf.caption(context.getString(R.string.pdf_footer_privacy))

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "fuel_${vehicle.id}_${System.currentTimeMillis()}.pdf")
        pdf.save(file)
        return file
    }
}
