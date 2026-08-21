package com.rovena.garage.presentation.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.databinding.ItemUpcomingTaskBinding
import com.rovena.garage.presentation.dashboard.UpcomingTaskUi
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.StatusColors

class UpcomingTaskAdapter : ListAdapter<UpcomingTaskUi, UpcomingTaskAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val binding: ItemUpcomingTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: UpcomingTaskUi) {
            val context = binding.root.context
            binding.taskTitle.text = task.title
            val color = ContextCompat.getColor(context, StatusColors.of(task.status))
            binding.statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            val statusLabel = context.getString(EnumLabels.of(task.status))
            val detail = when {
                task.remainingKm != null && task.remainingKm >= 0 -> "$statusLabel · ${task.remainingKm} KM"
                task.remainingDays != null && task.remainingDays >= 0 -> "$statusLabel · ${task.remainingDays}d"
                else -> statusLabel
            }
            binding.taskStatus.text = detail
            binding.taskStatus.setTextColor(color)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UpcomingTaskUi>() {
            override fun areItemsTheSame(oldItem: UpcomingTaskUi, newItem: UpcomingTaskUi) = oldItem.title == newItem.title
            override fun areContentsTheSame(oldItem: UpcomingTaskUi, newItem: UpcomingTaskUi) = oldItem == newItem
        }
    }
}
