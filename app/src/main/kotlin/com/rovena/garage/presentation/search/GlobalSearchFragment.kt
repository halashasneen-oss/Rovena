package com.rovena.garage.presentation.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.rovena.garage.databinding.FragmentGlobalSearchBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

class GlobalSearchFragment : Fragment(R.layout.fragment_global_search) {

    private var _binding: FragmentGlobalSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlobalSearchViewModel by viewModels { viewModelFactory { GlobalSearchViewModel(appContainer) } }

    private lateinit var adapter: GlobalSearchAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGlobalSearchBinding.bind(inflater.inflate(R.layout.fragment_global_search, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        adapter = GlobalSearchAdapter(onClick = { result -> navigateToResult(result) })
        binding.listRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecycler.adapter = adapter
        binding.emptyState.emptyTitle.text = getString(R.string.search_empty_title)
        binding.emptyState.emptyMessage.text = getString(R.string.search_empty_message)

        binding.searchInput.requestFocus()
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.results)
                    val showEmpty = state.query.length >= 2 && state.results.isEmpty()
                    binding.emptyState.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    binding.listRecycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun navigateToResult(result: GlobalSearchResult) {
        // Parts and Notes have no dedicated per-record form destination (they're
        // edited inline from their list screens), so those two route to the list
        // screen scoped to the record's vehicle rather than to a record form.
        val destination = when (result.type) {
            SearchResultType.VEHICLE -> R.id.vehicleHubFragment
            SearchResultType.MAINTENANCE -> R.id.maintenanceFormFragment
            SearchResultType.FUEL -> R.id.fuelFormFragment
            SearchResultType.EXPENSE -> R.id.expenseFormFragment
            SearchResultType.DOCUMENT -> R.id.documentFormFragment
            SearchResultType.PART -> R.id.partListFragment
            SearchResultType.NOTE -> R.id.vehicleNoteListFragment
        }
        val args = when (result.type) {
            SearchResultType.VEHICLE, SearchResultType.PART, SearchResultType.NOTE -> bundleOf("vehicleId" to result.vehicleId)
            else -> bundleOf("vehicleId" to result.vehicleId, "recordId" to result.id)
        }
        findNavController().navigate(destination, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
