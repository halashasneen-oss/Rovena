package com.rovena.garage.presentation.notes

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
import com.rovena.garage.data.local.entities.VehicleNoteEntity
import com.rovena.garage.databinding.DialogNoteEditBinding
import com.rovena.garage.databinding.FragmentGenericListBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.confirmDelete
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

class VehicleNoteListFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VehicleNoteListViewModel by viewModels {
        viewModelFactory { VehicleNoteListViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private lateinit var adapter: VehicleNoteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_notes)
        binding.screenSummary.visibility = View.GONE
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        adapter = VehicleNoteAdapter(
            onClick = { note -> showEditDialog(note) },
            onMoreClick = { note -> showOptions(note) }
        )
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter
        binding.emptyState.emptyTitle.text = getString(R.string.note_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.note_empty_message)

        binding.fabAdd.setOnClickListener { showEditDialog(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.notes)
                    val empty = !state.isLoading && state.notes.isEmpty()
                    binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showEditDialog(existing: VehicleNoteEntity?) {
        val vehicleId = viewModel.uiState.value.vehicleId ?: return
        val dialogBinding = DialogNoteEditBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.noteTextInput.setText(existing?.text.orEmpty())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.note_add_title else R.string.note_edit_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.save(vehicleId, existing, dialogBinding.noteTextInput.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showOptions(note: VehicleNoteEntity) {
        val popup = android.widget.PopupMenu(requireContext(), binding.root)
        popup.menuInflater.inflate(R.menu.reminder_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.option_edit -> showEditDialog(note)
                R.id.option_delete -> confirmDelete { viewModel.delete(note) }
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
