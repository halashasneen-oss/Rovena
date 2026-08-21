package com.rovena.garage.presentation.fuel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentGenericListBinding
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch

class FuelListFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FuelListViewModel by viewModels {
        viewModelFactory { FuelListViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private val adapter = FuelAdapter { record ->
        findNavController().navigate(R.id.fuelFormFragment, bundleOf("vehicleId" to record.vehicleId, "recordId" to record.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_fuel)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter
        binding.emptyState.emptyTitle.text = getString(R.string.fuel_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.fuel_empty_message)

        binding.fabAdd.setOnClickListener {
            val vehicleId = viewModel.uiState.value.vehicleId ?: return@setOnClickListener
            findNavController().navigate(R.id.fuelFormFragment, bundleOf("vehicleId" to vehicleId, "recordId" to 0L))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.records)
                    binding.screenSummary.text = Formatters.fuelEconomy(requireContext(), state.stats?.averageLitersPer100Km, FuelEconomyUnit.L_100KM)
                    val empty = !state.isLoading && state.records.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
