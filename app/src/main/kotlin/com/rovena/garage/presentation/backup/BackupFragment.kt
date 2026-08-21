package com.rovena.garage.presentation.backup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentBackupBinding
import com.rovena.garage.domain.usecase.BackupVersionValidator
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.Formatters
import com.rovena.garage.utils.backup.BackupInspection
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class BackupFragment : Fragment(R.layout.fragment_backup) {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackupViewModel by viewModels { viewModelFactory { BackupViewModel(appContainer) } }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.createBackup(requireContext(), uri)
    }

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.inspectBackup(requireContext(), uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBackupBinding.bind(inflater.inflate(R.layout.fragment_backup, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        binding.createBackupButton.setOnClickListener {
            val fileName = "rovena_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(System.currentTimeMillis())}.motiva"
            createDocument.launch(fileName)
        }
        binding.restoreBackupButton.setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { history ->
                    val latest = history.firstOrNull()
                    binding.lastBackupText.text = latest?.let {
                        getString(R.string.backup_last_created, Formatters.date(requireContext(), it.createdAt), it.vehicleCount)
                    } ?: getString(R.string.backup_none_yet)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event -> event?.let { handleEvent(it) } }
            }
        }
    }

    private fun handleEvent(event: BackupUiEvent) {
        when (event) {
            is BackupUiEvent.BackupCreated -> toast(getString(R.string.backup_created_success, event.vehicleCount))
            is BackupUiEvent.BackupError -> toast(getString(R.string.backup_error, event.message))
            is BackupUiEvent.RestoreInvalid -> {
                val message = when (event.validation) {
                    is BackupVersionValidator.ValidationResult.UnsupportedVersion -> getString(R.string.backup_invalid_version)
                    else -> getString(R.string.backup_invalid_corrupt)
                }
                toast(message)
            }
            is BackupUiEvent.RestorePending -> showRestoreChoiceDialog(event.inspection)
            is BackupUiEvent.RestoreCompletedMerged -> toast(getString(R.string.backup_restore_completed_merged, event.vehicleCount))
            BackupUiEvent.RestoreCompletedNeedsRestart -> restartApp()
        }
        viewModel.consumeEvent()
    }

    private fun showRestoreChoiceDialog(inspection: BackupInspection) {
        val vehicleCount = inspection.manifest?.vehicleCount ?: 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup_confirm_title)
            .setMessage(getString(R.string.backup_confirm_message, vehicleCount))
            .setPositiveButton(R.string.backup_restore_option_replace) { _, _ -> viewModel.restoreReplacing(requireContext(), inspection) }
            .setNeutralButton(R.string.backup_restore_option_new) { _, _ -> viewModel.restoreAsNewGarage(requireContext(), inspection) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun restartApp() {
        toast(getString(R.string.backup_restore_completed_restart))
        val context = requireContext().applicationContext
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent?.let { context.startActivity(it) }
        Runtime.getRuntime().exit(0)
    }

    private fun toast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
