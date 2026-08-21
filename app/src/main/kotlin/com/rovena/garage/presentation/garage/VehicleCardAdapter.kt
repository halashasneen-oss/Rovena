package com.rovena.garage.presentation.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rovena.garage.databinding.ItemVehicleCardBinding
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors
import java.io.File

class VehicleCardAdapter(
    private val onClick: (VehicleCardUi) -> Unit,
    private val onMoreClick: (VehicleCardUi, android.view.View) -> Unit
) : ListAdapter<VehicleCardUi, VehicleCardAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVehicleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemVehicleCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: VehicleCardUi) {
            val context = binding.root.context
            binding.vehicleTitle.text = "${card.vehicle.make} ${card.vehicle.model}"
            binding.vehicleSubtitle.text = "${card.vehicle.year} · ${Formatters.mileage(context, card.vehicle.currentMileageKm, com.rovena.garage.domain.model.DistanceUnit.KM)}"
            binding.primaryBadge.visibility = if (card.vehicle.isPrimary) android.view.View.VISIBLE else android.view.View.GONE
            binding.healthScoreText.text = card.healthScore?.toString() ?: "--"
            val color = ContextCompat.getColor(context, StatusColors.of(card.healthStatus))
            binding.healthScoreText.setTextColor(color)
            card.vehicle.photoPath?.let { path ->
                binding.vehiclePhoto.load(File(path)) { crossfade(true) }
            }
            binding.root.setOnClickListener { onClick(card) }
            binding.moreButton.setOnClickListener { onMoreClick(card, binding.moreButton) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VehicleCardUi>() {
            override fun areItemsTheSame(oldItem: VehicleCardUi, newItem: VehicleCardUi) = oldItem.vehicle.id == newItem.vehicle.id
            override fun areContentsTheSame(oldItem: VehicleCardUi, newItem: VehicleCardUi) = oldItem == newItem
        }
    }
}
