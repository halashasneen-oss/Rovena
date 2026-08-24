package com.rovena.garage.presentation.fuel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.databinding.ItemRecordRowBinding
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.utils.Formatters

class FuelAdapter(private val onClick: (FuelRecordEntity) -> Unit) : ListAdapter<FuelRecordEntity, FuelAdapter.VH>(DIFF) {

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
        fun bind(record: FuelRecordEntity) {
            val context = binding.root.context
            binding.recordTitle.text = "${record.liters} L" + if (record.isFullTank) "" else " · " + context.getString(com.rovena.garage.R.string.fuel_partial)
            binding.recordSubtitle.text = "${Formatters.date(context, record.dateMillis)} · ${Formatters.mileage(context, record.mileageKm, distanceUnit)}"
            binding.recordAmount.text = Formatters.currency(context, record.totalCost, record.currencyCode)
            binding.recordStatus.visibility = android.view.View.GONE
            binding.root.setOnClickListener { onClick(record) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FuelRecordEntity>() {
            override fun areItemsTheSame(oldItem: FuelRecordEntity, newItem: FuelRecordEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FuelRecordEntity, newItem: FuelRecordEntity) = oldItem == newItem
        }
    }
}
