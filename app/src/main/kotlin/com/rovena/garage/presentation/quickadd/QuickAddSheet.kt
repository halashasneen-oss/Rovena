package com.rovena.garage.presentation.quickadd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentQuickAddSheetBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

class QuickAddSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentQuickAddSheetBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong(ARG_VEHICLE_ID, 0L) ?: 0L }

    private val viewModel: QuickAddViewModel by viewModels { viewModelFactory { QuickAddViewModel(appContainer) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuickAddSheetBinding.bind(inflater.inflate(R.layout.fragment_quick_add_sheet, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindOption(binding.optionFuel, R.drawable.ic_fuel, R.string.quick_action_fuel) { navigateAndDismiss(R.id.fuelFormFragment) }
        bindOption(binding.optionMaintenance, R.drawable.ic_service, R.string.quick_action_maintenance) { navigateAndDismiss(R.id.maintenanceFormFragment) }
        bindOption(binding.optionExpense, R.drawable.ic_expense, R.string.quick_action_expense) { navigateAndDismiss(R.id.expenseFormFragment) }
        bindOption(binding.optionDocument, R.drawable.ic_document, R.string.quick_action_document) { navigateAndDismiss(R.id.documentFormFragment) }
        bindOption(binding.optionInspection, R.drawable.ic_inspection, R.string.quick_action_inspection) { navigateAndDismiss(R.id.inspectionFormFragment) }
        bindOption(binding.optionReminder, R.drawable.ic_reminder, R.string.quick_action_reminder) { navigateAndDismiss(R.id.reminderFormFragment) }
        bindOption(binding.optionNote, R.drawable.ic_document, R.string.quick_action_note) { showNoteDialog() }
    }

    private fun navigateAndDismiss(destinationId: Int) {
        val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navHostFragment.navController.navigate(destinationId, bundleOf("vehicleId" to vehicleId, "recordId" to 0L))
        dismiss()
    }

    private fun showNoteDialog() {
        val input = EditText(requireContext()).apply { hint = getString(R.string.quick_add_note_hint) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.quick_action_note)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch { viewModel.addNote(vehicleId, text) }
                }
                dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun bindOption(included: com.rovena.garage.databinding.ItemQuickActionBinding, icon: Int, label: Int, onClick: () -> Unit) {
        included.quickActionIcon.setImageResource(icon)
        included.quickActionLabel.setText(label)
        included.quickActionRoot.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "QuickAddSheet"
        private const val ARG_VEHICLE_ID = "vehicleId"

        fun newInstance(vehicleId: Long) = QuickAddSheet().apply {
            arguments = bundleOf(ARG_VEHICLE_ID to vehicleId)
        }
    }
}
