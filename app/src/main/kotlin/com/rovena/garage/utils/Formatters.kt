package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.domain.usecase.CurrencyAggregator
import com.rovena.garage.domain.usecase.MileageValidator
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Locale-aware number/date/unit formatting shared by every screen. */
object Formatters {

    /** Null when the check found nothing worth flagging (Ok, or no previous reading yet). */
    fun mileageWarningText(context: Context, check: MileageValidator.MileageCheck): String? = when (check) {
        is MileageValidator.MileageCheck.Ok -> null
        is MileageValidator.MileageCheck.LowerThanPrevious ->
            context.getString(R.string.mileage_warning_lower, check.previousMileageKm, check.enteredMileageKm)
        is MileageValidator.MileageCheck.UnrealisticJump ->
            context.getString(R.string.mileage_warning_jump, check.previousMileageKm, check.enteredMileageKm, check.deltaKm)
    }

    fun mileage(context: Context, km: Int, unit: DistanceUnit): String {
        val locale = context.resources.configuration.locales[0]
        val value = if (unit == DistanceUnit.MILES) kmToMiles(km) else km.toDouble()
        val formatted = NumberFormat.getIntegerInstance(locale).format(value.roundToInt())
        val unitLabel = if (unit == DistanceUnit.MILES) context.getString(R.string.unit_miles_short) else context.getString(R.string.unit_km_short)
        return "$formatted $unitLabel"
    }

    fun distance(context: Context, km: Int, unit: DistanceUnit): String = mileage(context, km, unit)

    fun kmToMiles(km: Int): Double = km * 0.621371
    fun kmToMiles(km: Double): Double = km * 0.621371
    fun milesToKm(miles: Double): Int = (miles / 0.621371).roundToInt()

    fun currency(context: Context, amount: Double, currency: AppCurrency, customCode: String?): String {
        val locale = context.resources.configuration.locales[0]
        val code = EnumLabels.currencySymbolOrCode(currency, customCode)
        val formatted = String.format(locale, "%,.2f", amount)
        return "$formatted $code"
    }

    /**
     * Formats a single financial record using the ISO-style code it was
     * actually stamped with (spec: currency architecture) - NOT the app's
     * current default currency, which may have changed since. Falls back to
     * JOD only for a record that somehow still has no currency recorded.
     */
    fun currency(context: Context, amount: Double, currencyCode: String?): String {
        val locale = context.resources.configuration.locales[0]
        val formatted = String.format(locale, "%,.2f", amount)
        return "$formatted ${currencyCode?.takeIf { it.isNotBlank() } ?: AppCurrency.JOD.code}"
    }

    /**
     * Formats an aggregated [CurrencyAggregator.CurrencyTotal] (spec: never sum
     * across currencies). A [CurrencyAggregator.CurrencyTotal.Mixed] result
     * renders as one line per currency rather than a single combined number.
     */
    fun currencyTotal(context: Context, total: CurrencyAggregator.CurrencyTotal): String = when (total) {
        is CurrencyAggregator.CurrencyTotal.Empty -> context.getString(R.string.not_enough_data)
        is CurrencyAggregator.CurrencyTotal.Single -> currency(context, total.amount, total.currencyCode)
        is CurrencyAggregator.CurrencyTotal.Mixed -> total.byCurrency.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (code, amount) -> currency(context, amount, code) }
    }

    /** Short one-line label for a mixed-currency total where only a compact summary fits (e.g. a stat card). */
    fun currencyTotalCompact(context: Context, total: CurrencyAggregator.CurrencyTotal): String = when (total) {
        is CurrencyAggregator.CurrencyTotal.Mixed -> context.getString(R.string.multiple_currencies)
        else -> currencyTotal(context, total)
    }

    fun fuelEconomy(context: Context, litersPer100Km: Double?, unit: FuelEconomyUnit): String {
        if (litersPer100Km == null) return context.getString(R.string.not_enough_data)
        val locale = context.resources.configuration.locales[0]
        return if (unit == FuelEconomyUnit.MPG) {
            val mpg = 235.215 / litersPer100Km
            String.format(locale, "%.1f %s", mpg, context.getString(R.string.unit_mpg_short))
        } else {
            String.format(locale, "%.1f %s", litersPer100Km, context.getString(R.string.unit_l_100km_short))
        }
    }

    fun date(context: Context, epochMillis: Long): String {
        val locale = context.resources.configuration.locales[0]
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    }

    fun dateShort(context: Context, epochMillis: Long): String {
        val locale = context.resources.configuration.locales[0]
        val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    }

    /**
     * Relative section-header label for a date-grouped list (Timeline 2.0):
     * "Today" / "Yesterday" for the two most recent days, [dateShort] (day +
     * month) for anything else this year, [date] (day + month + year) once
     * the entry is from a previous year.
     */
    fun timelineSectionLabel(context: Context, epochMillis: Long): String {
        val entryDate = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now()
        return when {
            entryDate == today -> context.getString(R.string.date_today)
            entryDate == today.minusDays(1) -> context.getString(R.string.date_yesterday)
            entryDate.year == today.year -> dateShort(context, epochMillis)
            else -> date(context, epochMillis)
        }
    }
}
