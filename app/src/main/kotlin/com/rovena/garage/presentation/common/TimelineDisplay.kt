package com.rovena.garage.presentation.common

import android.content.Context
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.utils.EnumLabels

/**
 * Timeline rows store `title` as either a fixed-vocabulary enum name
 * (maintenance category, fuel type) or free user text (expense description,
 * document name, reminder title) - see [com.rovena.garage.data.repository.TimelineSyncer].
 * This resolves the enum cases back to a localized label; free text passes through untouched.
 */
object TimelineDisplay {
    fun titleFor(context: Context, event: TimelineEventEntity): String = when (event.type) {
        TimelineEventType.MAINTENANCE -> runCatching { enumValueOf<MaintenanceCategory>(event.title) }
            .map { context.getString(EnumLabels.of(it)) }
            .getOrDefault(event.title)
        TimelineEventType.FUEL -> runCatching { enumValueOf<FuelType>(event.title) }
            .map { context.getString(R.string.timeline_fuel_title, context.getString(EnumLabels.of(it))) }
            .getOrDefault(context.getString(R.string.timeline_fuel_title_generic))
        TimelineEventType.INSPECTION -> context.getString(R.string.inspection_title)
        // An expense without a description falls back to its raw ExpenseCategory name (see
        // TimelineSyncer.upsertForExpense) - resolve that back to a localized label the same
        // way MAINTENANCE/FUEL do above; a genuine free-text description passes through as-is.
        TimelineEventType.EXPENSE -> runCatching { enumValueOf<ExpenseCategory>(event.title) }
            .map { context.getString(EnumLabels.of(it)) }
            .getOrDefault(event.title)
        TimelineEventType.DOCUMENT, TimelineEventType.REMINDER, TimelineEventType.VEHICLE_UPDATE ->
            event.title
    }
}
