package com.rovena.garage.presentation.parts

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.PartEntity
import com.rovena.garage.databinding.DialogPartEditBinding
import com.rovena.garage.databinding.FragmentGenericListBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.Formatters
import kotlinx.coroutines.launch

class PartListFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PartListViewModel by viewModels {
        viewModelFactory { PartListViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private lateinit var adapter: PartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_parts)
        binding.screenSummary.visibility = View.GONE
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        adapter = PartAdapter(
            onClick = { row -> showEditDialog(row.part) },
            onMoreClick = { row -> showOptions(row) }
        )
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter
        binding.emptyState.emptyTitle.text = getString(R.string.part_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.part_empty_message)

        binding.fabAdd.setOnClickListener { showEditDialog(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.rows)
                    val empty = !state.isLoading && state.rows.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showEditDialog(existing: PartEntity?) {
        val vehicleId = viewModel.uiState.value.vehicleId ?: return
        val dialogBinding = DialogPartEditBinding.inflate(LayoutInflater.from(requireContext()))

        var installedDateMillis = existing?.installedDateMillis ?: System.currentTimeMillis()
        var warrantyDateMillis = existing?.warrantyExpiryDateMillis

        fun refreshDateButtons() {
            dialogBinding.installedDateButton.text = getString(R.string.part_field_installed_date, Formatters.date(requireContext(), installedDateMillis))
            dialogBinding.warrantyDateButton.text = warrantyDateMillis?.let {
                getString(R.string.part_field_warranty_date, Formatters.date(requireContext(), it))
            } ?: getString(R.string.part_field_warranty_date_unset)
        }

        dialogBinding.partNameInput.setText(existing?.name.orEmpty())
        dialogBinding.installedMileageInput.setText(existing?.installedMileageKm?.toString().orEmpty())
        dialogBinding.warrantyMileageInput.setText(existing?.warrantyExpiryMileageKm?.toString().orEmpty())
        dialogBinding.notesInput.setText(existing?.notes.orEmpty())
        refreshDateButtons()

        dialogBinding.installedDateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "part_installed_date", installedDateMillis) { millis ->
                installedDateMillis = millis
                refreshDateButtons()
            }
        }
        dialogBinding.warrantyDateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "part_warranty_date", warrantyDateMillis) { millis ->
                warrantyDateMillis = millis
                refreshDateButtons()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.part_add_title else R.string.part_edit_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = dialogBinding.partNameInput.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) return@setPositiveButton
                val part = PartEntity(
                    vehicleId = vehicleId,
                    name = name,
                    installedDateMillis = installedDateMillis,
                    installedMileageKm = dialogBinding.installedMileageInput.text?.toString()?.toIntOrNull(),
                    warrantyExpiryDateMillis = warrantyDateMillis,
                    warrantyExpiryMileageKm = dialogBinding.warrantyMileageInput.text?.toString()?.toIntOrNull(),
                    notes = dialogBinding.notesInput.text?.toString()?.trim()?.ifBlank { null }
                )
                viewModel.save(vehicleId, existing, part)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showOptions(row: PartRowUi) {
        val popup = android.widget.PopupMenu(requireContext(), binding.root)
        popup.menuInflater.inflate(R.menu.reminder_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.option_edit -> showEditDialog(row.part)
                R.id.option_delete -> viewModel.delete(row.part)
            }
            true
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
