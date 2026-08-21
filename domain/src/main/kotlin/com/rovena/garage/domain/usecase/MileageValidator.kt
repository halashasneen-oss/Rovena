package com.rovena.garage.domain.usecase

/**
 * Detects suspicious mileage entries (spec #38 Data Consistency) without ever
 * silently correcting them - the user always makes the final call.
 */
object MileageValidator {

    sealed class MileageCheck {
        data object Ok : MileageCheck()
        data class LowerThanPrevious(val previousMileageKm: Int, val enteredMileageKm: Int) : MileageCheck()
        data class UnrealisticJump(val previousMileageKm: Int, val enteredMileageKm: Int, val deltaKm: Int) : MileageCheck()
    }

    /** A single-day jump larger than this is flagged for user confirmation, not blocked. */
    const val MAX_PLAUSIBLE_DAILY_JUMP_KM = 2000

    fun check(newMileageKm: Int, previousMileageKm: Int?, daysSincePrevious: Long? = null): MileageCheck {
        if (previousMileageKm == null) return MileageCheck.Ok
        if (newMileageKm < previousMileageKm) {
            return MileageCheck.LowerThanPrevious(previousMileageKm, newMileageKm)
        }
        val delta = newMileageKm - previousMileageKm
        val days = (daysSincePrevious ?: 1L).coerceAtLeast(1L)
        val plausibleMax = MAX_PLAUSIBLE_DAILY_JUMP_KM * days
        if (delta > plausibleMax) {
            return MileageCheck.UnrealisticJump(previousMileageKm, newMileageKm, delta)
        }
        return MileageCheck.Ok
    }
}
