package com.rovena.garage.presentation.inspection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.databinding.ItemRecordRowBinding
import com.rovena.garage.domain.usecase.HealthScoreCalculator
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors

class InspectionAdapter(private val onClick: (InspectionEntity) -> Unit) : ListAdapter<InspectionEntity, InspectionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemRecordRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(inspection: InspectionEntity) {
            val context = binding.root.context
            binding.recordTitle.text = context.getString(R.string.inspection_title)
            binding.recordSubtitle.text = "${Formatters.date(context, inspection.dateMillis)} · ${Formatters.mileage(context, inspection.mileageKm, com.rovena.garage.domain.model.DistanceUnit.KM)}"
            binding.recordAmount.text = inspection.overallScore?.let { "$it / 100" } ?: context.getString(R.string.not_enough_data)
            if (inspection.overallScore != null) {
                binding.recordStatus.visibility = android.view.View.VISIBLE
                val status = HealthScoreCalculator.statusFor(inspection.overallScore)
                binding.recordStatus.text = context.getString(EnumLabels.of(status))
                binding.recordStatus.setTextColor(ContextCompat.getColor(context, StatusColors.of(status)))
            } else {
                binding.recordStatus.visibility = android.view.View.GONE
            }
            binding.root.setOnClickListener { onClick(inspection) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<InspectionEntity>() {
            override fun areItemsTheSame(oldItem: InspectionEntity, newItem: InspectionEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: InspectionEntity, newItem: InspectionEntity) = oldItem == newItem
        }
    }
}
