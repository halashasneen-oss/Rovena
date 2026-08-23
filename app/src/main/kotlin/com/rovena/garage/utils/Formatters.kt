package com.rovena.garage.utils

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Locale-aware number/date/unit formatting shared by every screen. */
object Formatters {

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
