package com.rovena.garage.domain.usecase

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Centralized field-validation rules (spec: input validation) so "no negative
 * mileage/cost/quantity", "no impossible date" etc. are defined once instead
 * of redefined ad-hoc per form screen. Every function only *reports* whether
 * a value is acceptable - never silently clamps, rounds, or rewrites it.
 */
object InputValidator {

    enum class Error {
        REQUIRED,
        NEGATIVE_MILEAGE,
        NEGATIVE_COST,
        NOT_POSITIVE_QUANTITY,
        NOT_POSITIVE_ENGINE_SIZE,
        IMPLAUSIBLE_DATE,
        INVALID_YEAR,
        INVALID_VIN
    }

    /** A required mileage reading - null or negative is rejected. */
    fun mileageKm(value: Int?): Error? = when {
        value == null -> Error.REQUIRED
        value < 0 -> Error.NEGATIVE_MILEAGE
        else -> null
    }

    /** An optional mileage field (next-due mileage, reminder mileage, warranty mileage) - absent is fine, negative is not. */
    fun optionalMileageKm(value: Int?): Error? = if (value != null && value < 0) Error.NEGATIVE_MILEAGE else null

    /** A required quantity that must be strictly positive (fuel liters). */
    fun positiveQuantity(value: Double?): Error? = when {
        value == null -> Error.REQUIRED
        value <= 0 -> Error.NOT_POSITIVE_QUANTITY
        else -> null
    }

    /** A required monetary amount - zero is allowed (e.g. a free/warranty service), negative is not. */
    fun cost(value: Double?): Error? = when {
        value == null -> Error.REQUIRED
        value < 0 -> Error.NEGATIVE_COST
        else -> null
    }

    /** An optional monetary amount (maintenance cost, repair estimate, purchase price, estimated value, fuel price per liter) - absent is fine, negative is not. */
    fun optionalCost(value: Double?): Error? = if (value != null && value < 0) Error.NEGATIVE_COST else null

    /** Optional engine size in liters - must be strictly positive when present (zero/negative makes no physical sense). */
    fun optionalEngineSize(value: Double?): Error? = if (value != null && value <= 0) Error.NOT_POSITIVE_ENGINE_SIZE else null

    fun requiredText(value: String): Error? = if (value.isBlank()) Error.REQUIRED else null

    /**
     * Rejects a date further than [maxYearsInPast] behind or [maxYearsInFuture]
     * ahead of [today] - catches an obvious fat-finger year (a stray extra
     * digit) without being so strict a legitimately old vehicle record gets
     * rejected. Generous by design: this flags only genuinely implausible
     * dates, not "unusual but real" ones.
     */
    fun plausibleDate(epochMillis: Long, today: LocalDate = LocalDate.now(), maxYearsInPast: Int = 100, maxYearsInFuture: Int = 1): Error? {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return if (date.isBefore(today.minusYears(maxYearsInPast.toLong())) || date.isAfter(today.plusYears(maxYearsInFuture.toLong()))) {
            Error.IMPLAUSIBLE_DATE
        } else null
    }

    /** A vehicle model year - required, and bounded to a plausible automotive range. */
    fun vehicleYear(year: Int?, today: LocalDate = LocalDate.now()): Error? = when {
        year == null -> Error.REQUIRED
        year < 1900 || year > today.year + 1 -> Error.INVALID_YEAR
        else -> null
    }

    /**
     * Loose VIN sanity check - not a manufacturer checksum validator, just a
     * shape check: 11-17 alphanumeric characters, excluding I/O/Q (never used
     * in real VINs, reserved to avoid confusion with 1/0). Only applied when
     * non-blank - VIN is an optional field.
     */
    fun vin(value: String): Error? {
        if (value.isBlank()) return null
        val trimmed = value.trim().uppercase()
        return if (!trimmed.matches(Regex("[A-HJ-NPR-Z0-9]{11,17}"))) Error.INVALID_VIN else null
    }
}
