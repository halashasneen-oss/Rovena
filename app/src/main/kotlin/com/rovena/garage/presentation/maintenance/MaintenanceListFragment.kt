package com.rovena.garage.presentation.maintenance

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
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.MaintenancePdfGenerator
import com.rovena.garage.utils.PdfViewerLauncher
import kotlinx.coroutines.launch

class MaintenanceListFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaintenanceListViewModel by viewModels {
        viewModelFactory { MaintenanceListViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private val adapter = MaintenanceAdapter { row ->
        findNavController().navigate(R.id.maintenanceFormFragment, bundleOf("vehicleId" to row.record.vehicleId, "recordId" to row.record.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_maintenance)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter

        binding.emptyState.emptyTitle.text = getString(R.string.maintenance_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.maintenance_empty_message)

        binding.fabAdd.setOnClickListener {
            val vehicleId = viewModel.uiState.value.vehicleId ?: return@setOnClickListener
            findNavController().navigate(R.id.maintenanceFormFragment, bundleOf("vehicleId" to vehicleId, "recordId" to 0L))
        }

        binding.pdfButton.visibility = View.VISIBLE
        binding.pdfButton.setOnClickListener { generatePdf() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.rows)
                    binding.screenSummary.text = Formatters.currency(requireContext(), state.totalCost, AppCurrency.JOD, null)
                    val empty = !state.isLoading && state.rows.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                    binding.pdfButton.isEnabled = state.rows.isNotEmpty()
                }
            }
        }
    }

    private fun generatePdf() {
        val vehicleId = viewModel.uiState.value.vehicleId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val vehicle = appContainer.vehicleRepository.getById(vehicleId) ?: return@launch
            val records = viewModel.uiState.value.rows.map { it.record }
            val file = MaintenancePdfGenerator.generate(requireContext(), vehicle, records)
            PdfViewerLauncher.open(this@MaintenanceListFragment, file)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
