package com.rovena.garage.presentation.maintenance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.databinding.ItemRecordRowBinding
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors

class MaintenanceAdapter(private val onClick: (MaintenanceRowUi) -> Unit) :
    ListAdapter<MaintenanceRowUi, MaintenanceAdapter.VH>(DIFF) {

    private var distanceUnit: DistanceUnit = DistanceUnit.KM

    fun setDistanceUnit(unit: DistanceUnit) {
        distanceUnit = unit
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemRecordRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: MaintenanceRowUi) {
            val context = binding.root.context
            binding.recordTitle.text = context.getString(EnumLabels.of(row.record.category))
            binding.recordSubtitle.text = "${Formatters.date(context, row.record.dateMillis)} · ${Formatters.mileage(context, row.record.mileageKm, distanceUnit)}"
            binding.recordAmount.text = row.record.cost?.let {
                Formatters.currency(context, it, row.record.currencyCode)
            } ?: ""
            if (row.dueStatus != null) {
                binding.recordStatus.visibility = android.view.View.VISIBLE
                binding.recordStatus.text = context.getString(EnumLabels.of(row.dueStatus))
                binding.recordStatus.setTextColor(ContextCompat.getColor(context, StatusColors.of(row.dueStatus)))
            } else {
                binding.recordStatus.visibility = android.view.View.GONE
            }
            binding.root.setOnClickListener { onClick(row) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MaintenanceRowUi>() {
            override fun areItemsTheSame(oldItem: MaintenanceRowUi, newItem: MaintenanceRowUi) = oldItem.record.id == newItem.record.id
            override fun areContentsTheSame(oldItem: MaintenanceRowUi, newItem: MaintenanceRowUi) = oldItem == newItem
        }
    }
}
