package com.rovena.garage.presentation.garage

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentGarageBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

class GarageFragment : Fragment(R.layout.fragment_garage) {

    private var _binding: FragmentGarageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GarageViewModel by viewModels { viewModelFactory { GarageViewModel(appContainer) } }

    private val adapter = VehicleCardAdapter(
        onClick = { card -> navigateToHub(card.vehicle.id) },
        onMoreClick = { card, anchor -> showOptions(card, anchor) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGarageBinding.bind(inflater.inflate(R.layout.fragment_garage, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vehiclesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.vehiclesRecycler.adapter = adapter

        binding.emptyState.emptyTitle.text = getString(R.string.garage_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.garage_empty_message)
        binding.emptyState.emptyAction.text = getString(R.string.add_vehicle)
        binding.emptyState.emptyAction.visibility = View.VISIBLE
        binding.emptyState.emptyAction.setOnClickListener { navigateToForm(0L) }

        binding.fabAddVehicle.setOnClickListener { navigateToForm(0L) }
        binding.searchButton.setOnClickListener { findNavController().navigate(R.id.globalSearchFragment) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.vehicles)
                    val empty = !state.isLoading && state.vehicles.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.vehiclesRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showOptions(card: VehicleCardUi, anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.vehicle_card_options, popup.menu)
        popup.menu.findItem(R.id.option_set_primary).isVisible = !card.vehicle.isPrimary
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.option_edit -> navigateToForm(card.vehicle.id)
                R.id.option_set_primary -> viewModel.setPrimary(card.vehicle.id)
                R.id.option_delete -> confirmDelete(card)
            }
            true
        }
        popup.show()
    }

    private fun confirmDelete(card: VehicleCardUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_vehicle_confirm_title)
            .setMessage(R.string.delete_vehicle_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(card.vehicle) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun navigateToForm(vehicleId: Long) {
        findNavController().navigate(R.id.vehicleFormFragment, bundleOf("vehicleId" to vehicleId))
    }

    private fun navigateToHub(vehicleId: Long) {
        findNavController().navigate(R.id.vehicleHubFragment, bundleOf("vehicleId" to vehicleId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
