package com.rovena.garage.presentation.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentGenericListBinding
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.presentation.common.TimelineEventAdapter
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

class TimelineFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimelineViewModel by viewModels {
        viewModelFactory { TimelineViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private val adapter = TimelineEventAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_timeline)
        binding.screenSummary.visibility = View.GONE
        binding.fabAdd.visibility = View.GONE
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter
        binding.emptyState.emptyTitle.text = getString(R.string.timeline_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.timeline_empty_message)

        setupFilters()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.events)
                    val empty = !state.isLoading && state.events.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun setupFilters() {
        binding.filterChipGroup.visibility = View.VISIBLE
        val options = listOf<Pair<String, TimelineEventType?>>(
            getString(R.string.timeline_filter_all) to null,
            getString(R.string.hub_section_maintenance) to TimelineEventType.MAINTENANCE,
            getString(R.string.hub_section_fuel) to TimelineEventType.FUEL,
            getString(R.string.hub_section_expenses) to TimelineEventType.EXPENSE,
            getString(R.string.hub_section_documents) to TimelineEventType.DOCUMENT,
            getString(R.string.hub_section_inspection) to TimelineEventType.INSPECTION
        )
        options.forEachIndexed { index, (label, type) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = index == 0
                id = View.generateViewId()
            }
            chip.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setFilter(type) }
            binding.filterChipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
