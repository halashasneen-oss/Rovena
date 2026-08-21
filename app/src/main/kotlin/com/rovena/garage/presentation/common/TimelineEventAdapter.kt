package com.rovena.garage.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.databinding.ItemTimelineEventBinding
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.utils.Formatters

class TimelineEventAdapter(
    private var distanceUnit: DistanceUnit = DistanceUnit.KM,
    private val onClick: (TimelineEventEntity) -> Unit = {}
) : ListAdapter<TimelineEventEntity, TimelineEventAdapter.VH>(DIFF) {

    fun setDistanceUnit(unit: DistanceUnit) {
        distanceUnit = unit
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTimelineEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemTimelineEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: TimelineEventEntity) {
            val context = binding.root.context
            binding.eventIcon.setImageResource(iconFor(event.type))
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
        private val DIFF = object : DiffUtil.ItemCallback<TimelineEventEntity>() {
            override fun areItemsTheSame(oldItem: TimelineEventEntity, newItem: TimelineEventEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: TimelineEventEntity, newItem: TimelineEventEntity) = oldItem == newItem
        }

        fun iconFor(type: TimelineEventType): Int = when (type) {
            TimelineEventType.FUEL -> com.rovena.garage.R.drawable.ic_fuel
            TimelineEventType.MAINTENANCE -> com.rovena.garage.R.drawable.ic_service
            TimelineEventType.EXPENSE -> com.rovena.garage.R.drawable.ic_expense
            TimelineEventType.DOCUMENT -> com.rovena.garage.R.drawable.ic_document
            TimelineEventType.INSPECTION -> com.rovena.garage.R.drawable.ic_inspection
            TimelineEventType.REMINDER -> com.rovena.garage.R.drawable.ic_reminder
            TimelineEventType.VEHICLE_UPDATE -> com.rovena.garage.R.drawable.ic_car_placeholder
        }
    }
}
