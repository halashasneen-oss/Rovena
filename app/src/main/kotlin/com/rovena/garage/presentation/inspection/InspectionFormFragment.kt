package com.rovena.garage.presentation.inspection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentInspectionFormBinding
import com.rovena.garage.databinding.ItemInspectionCheckBinding
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.DatePickerHelper
import com.rovena.garage.utils.EnumLabels
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.InspectionPdfGenerator
import com.rovena.garage.utils.StatusColors
import kotlinx.coroutines.launch

class InspectionFormFragment : Fragment(R.layout.fragment_inspection_form) {

    private var _binding: FragmentInspectionFormBinding? = null
    private val binding get() = _binding!!

    private val vehicleId: Long by lazy { arguments?.getLong("vehicleId", 0L) ?: 0L }
    private val recordId: Long by lazy { arguments?.getLong("recordId", 0L) ?: 0L }

    private val viewModel: InspectionFormViewModel by viewModels {
        viewModelFactory { InspectionFormViewModel(appContainer, vehicleId, recordId) }
    }

    private var isBinding = false
    private var rowsBuilt = false
    private val rowBindings = mutableMapOf<InspectionItemKey, ItemInspectionCheckBinding>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInspectionFormBinding.bind(inflater.inflate(R.layout.fragment_inspection_form, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dateButton.setOnClickListener {
            DatePickerHelper.show(childFragmentManager, "inspection_date", viewModel.state.value.dateMillis) { millis ->
                viewModel.update { it.copy(dateMillis = millis) }
            }
        }

        binding.mileageInput.addTextChangedListener(simpleWatcher { text -> if (!isBinding) viewModel.update { it.copy(mileage = text) } })
        binding.notesInput.addTextChangedListener(simpleWatcher { text -> if (!isBinding) viewModel.update { it.copy(notes = text) } })

        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener { viewModel.delete() }
        binding.pdfButton.setOnClickListener { generatePdf() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: InspectionFormState) {
        if (state.isLoading) return

        if (!rowsBuilt) {
            rowsBuilt = true
            buildRows(state)
        }

        isBinding = true
        if (binding.mileageInput.text?.toString() != state.mileage) binding.mileageInput.setText(state.mileage)
        if (binding.notesInput.text?.toString() != state.notes) binding.notesInput.setText(state.notes)
        isBinding = false

        binding.dateButton.text = Formatters.date(requireContext(), state.dateMillis)
        binding.mileageLayout.error = state.errors["mileage"]?.let { getString(it) }
        binding.deleteButton.visibility = if (state.id != 0L) View.VISIBLE else View.GONE
        binding.pdfButton.visibility = if (state.id != 0L) View.VISIBLE else View.GONE

        val score = state.liveScoreResult
        binding.scoreRing.progress = score.score ?: 0
        binding.scoreText.text = score.score?.toString() ?: "--"
        val status = score.score?.let { com.rovena.garage.domain.usecase.HealthScoreCalculator.statusFor(it) }
            ?: com.rovena.garage.domain.model.HealthStatus.NOT_ENOUGH_DATA
        val color = ContextCompat.getColor(requireContext(), StatusColors.of(status))
        binding.scoreRing.ringColor = color
        binding.scoreBreakdown.text = listOf(
            getString(R.string.inspection_issues_found, score.problemCount),
            getString(R.string.inspection_attention_items, score.attentionCount),
            getString(R.string.inspection_good_items, score.goodCount)
        ).joinToString(" · ")

        // Keep chip selection in sync (e.g. after initial load populates saved statuses).
        state.items.forEach { item ->
            rowBindings[item.itemKey]?.let { row -> syncChipSelection(row, item.status) }
        }

        if (state.isSaved || state.isDeleted) findNavController().popBackStack()
    }

    private fun buildRows(state: InspectionFormState) {
        val grouped = state.items.groupBy { it.categoryGroup }
        grouped[InspectionCategoryGroup.EXTERIOR]?.forEach { addRow(binding.exteriorContainer, it) }
        grouped[InspectionCategoryGroup.INTERIOR]?.forEach { addRow(binding.interiorContainer, it) }
        grouped[InspectionCategoryGroup.MECHANICAL]?.forEach { addRow(binding.mechanicalContainer, it) }
    }

    private fun addRow(container: LinearLayout, item: InspectionItemDraft) {
        val row = ItemInspectionCheckBinding.inflate(LayoutInflater.from(requireContext()), container, false)
        row.itemName.text = getString(EnumLabels.of(item.itemKey))
        syncChipSelection(row, item.status)

        row.statusChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val newStatus = when (checkedIds.firstOrNull()) {
                row.chipGood.id -> InspectionItemStatus.GOOD
                row.chipAttention.id -> InspectionItemStatus.ATTENTION
                row.chipProblem.id -> InspectionItemStatus.PROBLEM
                else -> InspectionItemStatus.UNKNOWN
            }
            viewModel.updateItem(item.itemKey) { it.copy(status = newStatus) }
        }

        row.itemNotesButton.setOnClickListener { showNotesDialog(item.itemKey) }

        rowBindings[item.itemKey] = row
        container.addView(row.root)
    }

    private fun syncChipSelection(row: ItemInspectionCheckBinding, status: InspectionItemStatus) {
        val targetId = when (status) {
            InspectionItemStatus.GOOD -> row.chipGood.id
            InspectionItemStatus.ATTENTION -> row.chipAttention.id
            InspectionItemStatus.PROBLEM -> row.chipProblem.id
            InspectionItemStatus.UNKNOWN -> View.NO_ID
        }
        if (row.statusChipGroup.checkedChipId != targetId) {
            row.chipGood.isChecked = status == InspectionItemStatus.GOOD
            row.chipAttention.isChecked = status == InspectionItemStatus.ATTENTION
            row.chipProblem.isChecked = status == InspectionItemStatus.PROBLEM
        }
    }

    private fun showNotesDialog(itemKey: InspectionItemKey) {
        val current = viewModel.state.value.items.find { it.itemKey == itemKey } ?: return
        val dialogView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val notesInput = EditText(requireContext()).apply {
            hint = getString(R.string.inspection_notes_dialog_title)
            setText(current.notes)
        }
        val costInput = EditText(requireContext()).apply {
            hint = getString(R.string.inspection_field_estimated_cost)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(current.estimatedRepairCost)
        }
        dialogView.addView(notesInput)
        dialogView.addView(costInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(EnumLabels.of(itemKey)))
            .setView(dialogView)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.updateItem(itemKey) {
                    it.copy(notes = notesInput.text.toString(), estimatedRepairCost = costInput.text.toString())
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun generatePdf() {
        val state = viewModel.state.value
        viewLifecycleOwner.lifecycleScope.launch {
            val vehicle = appContainer.vehicleRepository.getById(state.vehicleId) ?: return@launch
            val file = InspectionPdfGenerator.generate(requireContext(), vehicle, state)
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { startActivity(intent) }
        }
    }

    private fun simpleWatcher(onChanged: (String) -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) = onChanged(s?.toString().orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rowBindings.clear()
        rowsBuilt = false
        _binding = null
    }
}
