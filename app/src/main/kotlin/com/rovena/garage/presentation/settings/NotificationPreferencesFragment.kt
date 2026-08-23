package com.rovena.garage.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.AppSettingsEntity
import com.rovena.garage.databinding.FragmentNotificationPreferencesBinding
import com.rovena.garage.databinding.ItemSettingsRowBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import kotlinx.coroutines.launch

/**
 * Per-severity and per-category notification toggles (spec: tiered
 * notification severity). Reuses [SettingsViewModel] rather than a
 * dedicated one - this screen edits the same `AppSettingsEntity` row,
 * just a different slice of its fields.
 */
class NotificationPreferencesFragment : Fragment(R.layout.fragment_notification_preferences) {

    private var _binding: FragmentNotificationPreferencesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory { SettingsViewModel(appContainer) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationPreferencesBinding.bind(inflater.inflate(R.layout.fragment_notification_preferences, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwitchRow(binding.rowSeverityCritical, R.string.notification_prefs_severity_critical, R.string.notification_prefs_severity_critical_subtitle)
        setupSwitchRow(binding.rowSeverityImportant, R.string.notification_prefs_severity_important, R.string.notification_prefs_severity_important_subtitle)
        setupSwitchRow(binding.rowSeverityUpcoming, R.string.notification_prefs_severity_upcoming, R.string.notification_prefs_severity_upcoming_subtitle)
        setupSwitchRow(binding.rowCategoryDocuments, R.string.notification_prefs_category_documents, R.string.notification_prefs_category_documents_subtitle)
        setupSwitchRow(binding.rowCategoryGeneral, R.string.notification_prefs_category_general, R.string.notification_prefs_category_general_subtitle)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { render(it) }
            }
        }
    }

    private fun render(settings: AppSettingsEntity) {
        setChecked(binding.rowSeverityCritical, settings.notifyCriticalEnabled) { viewModel.setNotifyCriticalEnabled(it) }
        setChecked(binding.rowSeverityImportant, settings.notifyImportantEnabled) { viewModel.setNotifyImportantEnabled(it) }
        setChecked(binding.rowSeverityUpcoming, settings.notifyUpcomingEnabled) { viewModel.setNotifyUpcomingEnabled(it) }
        setChecked(binding.rowCategoryDocuments, settings.notifyDocumentCategoryEnabled) { viewModel.setNotifyDocumentCategoryEnabled(it) }
        setChecked(binding.rowCategoryGeneral, settings.notifyGeneralCategoryEnabled) { viewModel.setNotifyGeneralCategoryEnabled(it) }
    }

    private fun setChecked(row: ItemSettingsRowBinding, checked: Boolean, onToggled: (Boolean) -> Unit) {
        row.rowSwitch.setOnCheckedChangeListener(null)
        row.rowSwitch.isChecked = checked
        row.rowSwitch.setOnCheckedChangeListener { _, isChecked -> onToggled(isChecked) }
    }

    private fun setupSwitchRow(row: ItemSettingsRowBinding, titleRes: Int, subtitleRes: Int) {
        row.rowChevron.visibility = View.GONE
        row.rowSwitch.visibility = View.VISIBLE
        row.rowTitle.text = getString(titleRes)
        row.rowSubtitle.text = getString(subtitleRes)
        row.root.setOnClickListener { row.rowSwitch.toggle() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
