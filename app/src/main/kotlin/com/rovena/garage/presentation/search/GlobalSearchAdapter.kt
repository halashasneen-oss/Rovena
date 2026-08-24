package com.rovena.garage.presentation.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rovena.garage.R
import com.rovena.garage.databinding.ItemSearchResultBinding

class GlobalSearchAdapter(private val onClick: (GlobalSearchResult) -> Unit) :
    ListAdapter<GlobalSearchResult, GlobalSearchAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: GlobalSearchResult) {
            val context = binding.root.context
            binding.resultTitle.text = result.title
            binding.resultSubtitle.text = result.subtitleCategoryRes?.let { "${result.subtitle} · ${context.getString(it)}" } ?: result.subtitle
            binding.resultTypeBadge.text = context.getString(typeLabel(result.type))
            binding.root.setOnClickListener { onClick(result) }
        }

        private fun typeLabel(type: SearchResultType): Int = when (type) {
            SearchResultType.VEHICLE -> R.string.nav_garage
            SearchResultType.MAINTENANCE -> R.string.hub_section_maintenance
            SearchResultType.FUEL -> R.string.hub_section_fuel
            SearchResultType.EXPENSE -> R.string.hub_section_expenses
            SearchResultType.DOCUMENT -> R.string.hub_section_documents
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GlobalSearchResult>() {
            override fun areItemsTheSame(oldItem: GlobalSearchResult, newItem: GlobalSearchResult) =
                oldItem.type == newItem.type && oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GlobalSearchResult, newItem: GlobalSearchResult) = oldItem == newItem
        }
    }
}
