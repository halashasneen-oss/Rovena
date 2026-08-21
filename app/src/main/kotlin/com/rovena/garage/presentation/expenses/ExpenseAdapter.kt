package com.rovena.garage.presentation.expenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.databinding.ItemRecordRowBinding
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters

class ExpenseAdapter(private val onClick: (ExpenseEntity) -> Unit) : ListAdapter<ExpenseEntity, ExpenseAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemRecordRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: ExpenseEntity) {
            val context = binding.root.context
            binding.recordTitle.text = record.description?.takeIf { it.isNotBlank() } ?: context.getString(EnumLabels.of(record.category))
            binding.recordSubtitle.text = "${Formatters.date(context, record.dateMillis)} · ${context.getString(EnumLabels.of(record.category))}"
            binding.recordAmount.text = Formatters.currency(context, record.amount, AppCurrency.JOD, record.currencyCode)
            binding.recordStatus.visibility = android.view.View.GONE
            binding.root.setOnClickListener { onClick(record) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExpenseEntity>() {
            override fun areItemsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity) = oldItem == newItem
        }
    }
}
