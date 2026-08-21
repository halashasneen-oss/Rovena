package com.rovena.garage.utils

import androidx.annotation.ColorRes
import com.rovena.garage.R
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.model.HealthStatus

object StatusColors {
    @ColorRes
    fun of(status: HealthStatus): Int = when (status) {
        HealthStatus.EXCELLENT -> R.color.rovena_status_excellent
        HealthStatus.GOOD -> R.color.rovena_status_good
        HealthStatus.FAIR -> R.color.rovena_status_fair
        HealthStatus.ATTENTION_NEEDED -> R.color.rovena_status_attention
        HealthStatus.CRITICAL, HealthStatus.NOT_ENOUGH_DATA -> R.color.rovena_status_critical
    }

    @ColorRes
    fun of(status: DueStatus): Int = when (status) {
        DueStatus.UPCOMING -> R.color.rovena_status_good
        DueStatus.DUE_SOON -> R.color.rovena_status_fair
        DueStatus.DUE -> R.color.rovena_status_attention
        DueStatus.OVERDUE -> R.color.rovena_status_critical
    }
}
