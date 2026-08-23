package com.rovena.garage.presentation.parts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.databinding.ItemPartRowBinding
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.StatusColors

class PartAdapter(
    private val onClick: (PartRowUi) -> Unit,
    private val onMoreClick: (PartRowUi) -> Unit
) : ListAdapter<PartRowUi, PartAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPartRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemPartRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: PartRowUi) {
            val context = binding.root.context
            binding.partName.text = row.part.name
            binding.partSubtitle.text = Formatters.date(context, row.part.installedDateMillis)

            val status = row.warrantyStatus
            if (status != null) {
                binding.partWarrantyBadge.visibility = View.VISIBLE
                binding.partWarrantyBadge.text = context.getString(EnumLabels.of(status))
                binding.partWarrantyBadge.setTextColor(ContextCompat.getColor(context, StatusColors.of(status)))
            } else {
                binding.partWarrantyBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(row) }
            binding.partMoreButton.setOnClickListener { onMoreClick(row) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PartRowUi>() {
            override fun areItemsTheSame(oldItem: PartRowUi, newItem: PartRowUi) = oldItem.part.id == newItem.part.id
            override fun areContentsTheSame(oldItem: PartRowUi, newItem: PartRowUi) = oldItem == newItem
        }
    }
}
