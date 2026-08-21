package com.rovena.garage.presentation.documents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.databinding.ItemRecordRowBinding
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters

class DocumentAdapter(private val onClick: (DocumentEntity) -> Unit) : ListAdapter<DocumentEntity, DocumentAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemRecordRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doc: DocumentEntity) {
            val context = binding.root.context
            binding.recordTitle.text = doc.name
            binding.recordSubtitle.text = context.getString(EnumLabels.of(doc.type))
            binding.recordAmount.text = ""
            val now = System.currentTimeMillis()
            if (doc.expiryDateMillis != null) {
                binding.recordStatus.visibility = android.view.View.VISIBLE
                val expired = doc.expiryDateMillis < now
                binding.recordStatus.text = if (expired) context.getString(R.string.document_expired) else Formatters.date(context, doc.expiryDateMillis)
                binding.recordStatus.setTextColor(
                    ContextCompat.getColor(context, if (expired) R.color.rovena_status_critical else R.color.rovena_status_good)
                )
            } else {
                binding.recordStatus.visibility = android.view.View.GONE
            }
            binding.root.setOnClickListener { onClick(doc) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DocumentEntity>() {
            override fun areItemsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity) = oldItem == newItem
        }
    }
}
