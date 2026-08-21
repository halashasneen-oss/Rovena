package com.rovena.garage.presentation.reminders

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
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.resolveVehicleId
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

class ReminderListFragment : Fragment(R.layout.fragment_generic_list) {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderListViewModel by viewModels {
        viewModelFactory { ReminderListViewModel(appContainer, resolveVehicleId(appContainer)) }
    }

    private lateinit var adapter: ReminderAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenericListBinding.bind(inflater.inflate(R.layout.fragment_generic_list, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = getString(R.string.hub_section_reminders)
        binding.screenSummary.visibility = View.GONE
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        adapter = ReminderAdapter(
            onToggleComplete = { row -> viewModel.markCompleted(row.reminder) },
            onMoreClick = { row, anchor -> showOptions(row) },
            onClick = { row -> navigateToForm(row.reminder.vehicleId, row.reminder.id) }
        )
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter
        binding.emptyState.emptyTitle.text = getString(R.string.reminder_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.reminder_empty_message)

        binding.fabAdd.setOnClickListener {
            val vehicleId = viewModel.uiState.value.vehicleId ?: return@setOnClickListener
            navigateToForm(vehicleId, 0L)
        }

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

    private fun showOptions(row: ReminderRowUi) {
        val popup = android.widget.PopupMenu(requireContext(), binding.root)
        popup.menuInflater.inflate(R.menu.reminder_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.option_edit -> navigateToForm(row.reminder.vehicleId, row.reminder.id)
                R.id.option_delete -> viewModel.delete(row.reminder)
            }
            true
        }
        popup.show()
    }

    private fun navigateToForm(vehicleId: Long, reminderId: Long) {
        findNavController().navigate(R.id.reminderFormFragment, bundleOf("vehicleId" to vehicleId, "recordId" to reminderId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
