package com.rovena.garage.presentation.timeline

import com.rovena.garage.data.local.entities.TimelineEventEntity

/**
 * Timeline 2.0: the flat event list is grouped into date sections before
 * being handed to [SectionedTimelineAdapter] - see
 * [TimelineFragment.groupByDateSection].
 */
sealed class TimelineListItem {
    data class Header(val label: String) : TimelineListItem()
    data class Event(val event: TimelineEventEntity) : TimelineListItem()
}
