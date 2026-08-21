package com.rovena.garage.presentation.reminders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.databinding.ItemReminderRowBinding
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.StatusColors

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
                val detail = when {
                    eval.remainingKm != null -> "$statusLabel · ${eval.remainingKm} KM"
                    eval.remainingDays != null -> "$statusLabel · ${eval.remainingDays}d"
                    else -> statusLabel
                }
                binding.reminderDetail.text = detail
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
