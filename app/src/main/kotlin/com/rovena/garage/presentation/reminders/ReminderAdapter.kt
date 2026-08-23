package com.rovena.garage.presentation.reminders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.R
import com.rovena.garage.databinding.ItemReminderRowBinding
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors
import java.time.ZoneId

class ReminderAdapter(
    private val onToggleComplete: (ReminderRowUi) -> Unit,
    private val onMoreClick: (ReminderRowUi, android.view.View) -> Unit,
    private val onClick: (ReminderRowUi) -> Unit
) : ListAdapter<ReminderRowUi, ReminderAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReminderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemReminderRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ReminderRowUi) {
            val context = binding.root.context
            binding.reminderTitle.text = row.reminder.title
            binding.completeCheckbox.setOnCheckedChangeListener(null)
            binding.completeCheckbox.isChecked = row.reminder.isCompleted
            binding.completeCheckbox.setOnCheckedChangeListener { _, _ -> onToggleComplete(row) }

            val eval = row.evaluation
            if (row.reminder.isCompleted) {
                binding.reminderDetail.text = ""
                binding.reminderDetail.visibility = android.view.View.GONE
            } else if (eval != null) {
                binding.reminderDetail.visibility = android.view.View.VISIBLE
                val statusLabel = context.getString(EnumLabels.of(eval.status))
                val remainingKm = eval.remainingKm
                val remainingDays = eval.remainingDays
                val detail = when {
                    remainingKm != null -> "$statusLabel · $remainingKm KM"
                    remainingDays != null -> "$statusLabel · ${remainingDays}d"
                    else -> statusLabel
                }
                // A mileage-only reminder has no date of its own - if we have enough driving
                // history to project one, show it as a labeled estimate rather than leaving
                // the user with just a raw km countdown.
                // estimatedDueDate is a nullable property of a data class declared in the
                // domain Gradle module, so Kotlin won't smart-cast it across module
                // boundaries even after a null check - capture it into a local val first.
                val estimatedDueDate = eval.estimatedDueDate
                val estimateSuffix = if (remainingDays == null && estimatedDueDate != null) {
                    val millis = estimatedDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    " · " + context.getString(R.string.mileage_estimated_date, Formatters.dateShort(context, millis))
                } else ""
                binding.reminderDetail.text = detail + estimateSuffix
                binding.reminderDetail.setTextColor(ContextCompat.getColor(context, StatusColors.of(eval.status)))
            } else {
                binding.reminderDetail.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(row) }
            binding.reminderMoreButton.setOnClickListener { onMoreClick(row, binding.reminderMoreButton) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReminderRowUi>() {
            override fun areItemsTheSame(oldItem: ReminderRowUi, newItem: ReminderRowUi) = oldItem.reminder.id == newItem.reminder.id
            override fun areContentsTheSame(oldItem: ReminderRowUi, newItem: ReminderRowUi) = oldItem == newItem
        }
    }
}
