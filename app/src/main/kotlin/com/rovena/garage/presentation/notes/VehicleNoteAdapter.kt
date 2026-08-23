package com.rovena.garage.presentation.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.data.local.entities.VehicleNoteEntity
import com.rovena.garage.databinding.ItemNoteRowBinding
import com.rovena.garage.utils.Formatters

class VehicleNoteAdapter(
    private val onClick: (VehicleNoteEntity) -> Unit,
    private val onMoreClick: (VehicleNoteEntity) -> Unit
) : ListAdapter<VehicleNoteEntity, VehicleNoteAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNoteRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemNoteRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: VehicleNoteEntity) {
            binding.noteText.text = note.text
            binding.noteDate.text = Formatters.date(binding.root.context, note.updatedAtMillis)
            binding.root.setOnClickListener { onClick(note) }
            binding.noteMoreButton.setOnClickListener { onMoreClick(note) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VehicleNoteEntity>() {
            override fun areItemsTheSame(oldItem: VehicleNoteEntity, newItem: VehicleNoteEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: VehicleNoteEntity, newItem: VehicleNoteEntity) = oldItem == newItem
        }
    }
}
