package com.rovena.garage.presentation.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.databinding.ItemTimelineEventBinding
import com.rovena.garage.databinding.ItemTimelineSectionHeaderBinding
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.presentation.common.TimelineDisplay
import com.rovena.garage.presentation.common.TimelineEventAdapter
import com.rovena.garage.utils.Formatters

/**
 * Timeline 2.0: renders [TimelineListItem]s (date section headers +
 * events) in one flat RecyclerView. A separate adapter from
 * [TimelineEventAdapter] on purpose - that one also backs the Dashboard's
 * short "recent activity" preview list, which has no business showing date
 * section headers.
 */
class SectionedTimelineAdapter(
    private var distanceUnit: DistanceUnit = DistanceUnit.KM,
    private val onClick: (TimelineEventEntity) -> Unit = {}
) : ListAdapter<TimelineListItem, RecyclerView.ViewHolder>(DIFF) {

    fun setDistanceUnit(unit: DistanceUnit) {
        distanceUnit = unit
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TimelineListItem.Header -> VIEW_TYPE_HEADER
        is TimelineListItem.Event -> VIEW_TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderVH(ItemTimelineSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            EventVH(ItemTimelineEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TimelineListItem.Header -> (holder as HeaderVH).bind(item)
            is TimelineListItem.Event -> (holder as EventVH).bind(item.event)
        }
    }

    inner class HeaderVH(private val binding: ItemTimelineSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TimelineListItem.Header) {
            binding.sectionLabel.text = header.label
        }
    }

    inner class EventVH(private val binding: ItemTimelineEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: TimelineEventEntity) {
            val context = binding.root.context
            binding.eventIcon.setImageResource(TimelineEventAdapter.iconFor(event.type))
            binding.eventTitle.text = TimelineDisplay.titleFor(context, event)
            binding.eventSubtitle.text = event.mileageKm?.let { Formatters.mileage(context, it, distanceUnit) } ?: ""
            binding.eventAmount.text = event.amount?.let { amount ->
                val code = event.currencyCode ?: ""
                if (code.isBlank()) String.format("%,.2f", amount) else String.format("%,.2f %s", amount, code)
            } ?: ""
            binding.eventDate.text = Formatters.dateShort(context, event.dateMillis)
            binding.root.setOnClickListener { onClick(event) }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EVENT = 1

        private val DIFF = object : DiffUtil.ItemCallback<TimelineListItem>() {
            override fun areItemsTheSame(oldItem: TimelineListItem, newItem: TimelineListItem): Boolean = when {
                oldItem is TimelineListItem.Header && newItem is TimelineListItem.Header -> oldItem.label == newItem.label
                oldItem is TimelineListItem.Event && newItem is TimelineListItem.Event -> oldItem.event.id == newItem.event.id
                else -> false
            }

            override fun areContentsTheSame(oldItem: TimelineListItem, newItem: TimelineListItem): Boolean = oldItem == newItem
        }
    }
}
