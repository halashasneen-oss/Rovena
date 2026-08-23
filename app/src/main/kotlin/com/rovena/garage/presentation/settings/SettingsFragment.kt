package com.rovena.garage.presentation.settings

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.BuildConfig
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.AppSettingsEntity
import com.rovena.garage.databinding.FragmentSettingsBinding
import com.rovena.garage.databinding.ItemSettingsRowBinding
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.utils.PinHasher
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory { SettingsViewModel(appContainer) } }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setNotificationsEnabled(granted)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.bind(inflater.inflate(R.layout.fragment_settings, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRow(binding.rowManageVehicles, getString(R.string.settings_row_manage_vehicles), getString(R.string.settings_row_manage_vehicles_subtitle), showSwitch = false) {
            findNavController().navigate(R.id.nav_garage)
        }
        setupRow(binding.rowNotificationPreferences, getString(R.string.settings_row_notification_preferences), getString(R.string.settings_row_notification_preferences_subtitle), showSwitch = false) {
            findNavController().navigate(R.id.notificationPreferencesFragment)
        }
        setupRow(binding.rowBackup, getString(R.string.settings_row_backup), getString(R.string.settings_row_backup_subtitle), showSwitch = false) {
            findNavController().navigate(R.id.backupFragment)
        }
        setupRow(binding.rowRestore, getString(R.string.settings_row_restore), getString(R.string.settings_row_restore_subtitle), showSwitch = false) {
            findNavController().navigate(R.id.backupFragment)
        }
        setupRow(binding.rowTheme, getString(R.string.settings_row_theme), "", showSwitch = false) { showThemeDialog() }
        setupRow(binding.rowDistanceUnit, getString(R.string.settings_row_distance_unit), "", showSwitch = false) { showDistanceUnitDialog() }
        setupRow(binding.rowFuelUnit, getString(R.string.settings_row_fuel_unit), "", showSwitch = false) { showFuelUnitDialog() }
        setupRow(binding.rowCurrency, getString(R.string.settings_row_currency), "", showSwitch = false) { showCurrencyDialog() }
        setupRow(binding.rowLanguage, getString(R.string.settings_row_language), "", showSwitch = false) { showLanguageDialog() }
        setupRow(binding.rowVersion, getString(R.string.settings_row_version), BuildConfig.VERSION_NAME, showSwitch = false, showChevron = false, onClick = null)
        setupRow(binding.rowPrivacy, getString(R.string.settings_row_privacy), "", showSwitch = false) { showTextDialog(R.string.privacy_title, R.string.privacy_body) }
        setupRow(binding.rowTerms, getString(R.string.settings_row_terms), "", showSwitch = false) { showTextDialog(R.string.terms_title, R.string.terms_body) }
        setupRow(binding.rowLicenses, getString(R.string.settings_row_licenses), "", showSwitch = false) { showTextDialog(R.string.licenses_title, R.string.licenses_body) }

        if (BuildConfig.SAMPLE_DATA_ENABLED) {
            binding.rowSampleData.root.visibility = View.VISIBLE
            setupRow(binding.rowSampleData, getString(R.string.settings_row_sample_data), "", showSwitch = false) {
                viewModel.generateSampleData()
            }
        }

        setupNotificationsSwitch()
        setupAppLockSwitch()
        setupBiometricSwitch()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings -> render(settings) }
            }
        }
    }

    private fun render(settings: AppSettingsEntity) {
        binding.rowNotifications.rowSubtitle.text = if (settings.notificationsEnabled) getString(R.string.settings_row_notifications_on) else getString(R.string.settings_row_notifications_off)
        binding.rowNotifications.rowSwitch.setOnCheckedChangeListener(null)
        binding.rowNotifications.rowSwitch.isChecked = settings.notificationsEnabled
        binding.rowNotifications.rowSwitch.setOnCheckedChangeListener { _, checked -> onNotificationsToggled(checked) }

        binding.rowNotificationPreferences.root.isEnabled = settings.notificationsEnabled
        binding.rowNotificationPreferences.rowTitle.alpha = if (settings.notificationsEnabled) 1f else 0.5f
        binding.rowNotificationPreferences.rowSubtitle.text = if (settings.notificationsEnabled)
            getString(R.string.settings_row_notification_preferences_subtitle)
        else
            getString(R.string.settings_row_notification_preferences_subtitle_disabled)

        binding.rowAppLock.rowSwitch.setOnCheckedChangeListener(null)
        binding.rowAppLock.rowSwitch.isChecked = settings.appLockEnabled
        binding.rowAppLock.rowSwitch.setOnCheckedChangeListener { _, checked -> onAppLockToggled(checked) }

        binding.rowBiometric.rowSwitch.setOnCheckedChangeListener(null)
        binding.rowBiometric.rowSwitch.isChecked = settings.biometricEnabled
        binding.rowBiometric.rowSwitch.isEnabled = settings.appLockEnabled
        binding.rowBiometric.rowSubtitle.text = if (!settings.appLockEnabled) getString(R.string.settings_row_biometric_subtitle_disabled) else ""
        binding.rowBiometric.rowSwitch.setOnCheckedChangeListener { _, checked -> viewModel.setBiometricEnabled(checked) }

        binding.rowTheme.rowSubtitle.text = when (settings.themeMode) {
            AppThemeMode.LIGHT -> getString(R.string.theme_light)
            AppThemeMode.DARK -> getString(R.string.theme_dark)
            AppThemeMode.SYSTEM -> getString(R.string.theme_system)
        }
        binding.rowDistanceUnit.rowSubtitle.text = if (settings.distanceUnit == DistanceUnit.KM) getString(R.string.unit_km_full) else getString(R.string.unit_miles_full)
        binding.rowFuelUnit.rowSubtitle.text = if (settings.fuelEconomyUnit == FuelEconomyUnit.L_100KM) getString(R.string.unit_l100km_full) else getString(R.string.unit_mpg_full)
        binding.rowCurrency.rowSubtitle.text = settings.customCurrencyCode?.takeIf { settings.currency == AppCurrency.CUSTOM } ?: settings.currency.code
        binding.rowLanguage.rowSubtitle.text = languageLabel(settings.language)
    }

    private fun languageLabel(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> getString(R.string.language_english)
        AppLanguage.ARABIC -> getString(R.string.language_arabic)
        AppLanguage.FRENCH -> getString(R.string.language_french)
        AppLanguage.SPANISH -> getString(R.string.language_spanish)
    }

    private fun setupNotificationsSwitch() {
        binding.rowNotifications.rowChevron.visibility = View.GONE
        binding.rowNotifications.rowSwitch.visibility = View.VISIBLE
        binding.rowNotifications.rowTitle.text = getString(R.string.settings_row_notifications)
        binding.rowNotifications.root.setOnClickListener { binding.rowNotifications.rowSwitch.toggle() }
    }

    private fun onNotificationsToggled(checked: Boolean) {
        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotificationsEnabled(checked)
        }
    }

    private fun setupAppLockSwitch() {
        binding.rowAppLock.rowChevron.visibility = View.GONE
        binding.rowAppLock.rowSwitch.visibility = View.VISIBLE
        binding.rowAppLock.rowTitle.text = getString(R.string.settings_row_app_lock)
        binding.rowAppLock.root.setOnClickListener { binding.rowAppLock.rowSwitch.toggle() }
    }

    private fun onAppLockToggled(checked: Boolean) {
        if (checked) {
            showSetPinDialog()
        } else {
            viewModel.disableAppLock()
        }
    }

    private fun setupBiometricSwitch() {
        binding.rowBiometric.rowChevron.visibility = View.GONE
        binding.rowBiometric.rowSwitch.visibility = View.VISIBLE
        binding.rowBiometric.rowTitle.text = getString(R.string.settings_row_biometric)
        binding.rowBiometric.root.setOnClickListener { if (binding.rowBiometric.rowSwitch.isEnabled) binding.rowBiometric.rowSwitch.toggle() }
    }

    private fun showSetPinDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val maxLengthFilter = arrayOf<android.text.InputFilter>(android.text.InputFilter.LengthFilter(PinHasher.MAX_PIN_LENGTH))
        val pinInput = EditText(requireContext()).apply {
            hint = getString(R.string.lock_pin_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = maxLengthFilter
        }
        val confirmInput = EditText(requireContext()).apply {
            hint = getString(R.string.lock_pin_confirm_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = maxLengthFilter
        }
        container.addView(pinInput)
        container.addView(confirmInput)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_row_app_lock)
            .setView(container)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel) { _, _ -> binding.rowAppLock.rowSwitch.isChecked = false }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text.toString()
                val confirm = confirmInput.text.toString()
                if (pin.length !in PinHasher.MIN_PIN_LENGTH..PinHasher.MAX_PIN_LENGTH) {
                    pinInput.error = getString(R.string.lock_pin_too_short)
                    return@setOnClickListener
                }
                if (pin != confirm) {
                    confirmInput.error = getString(R.string.lock_pin_mismatch)
                    return@setOnClickListener
                }
                viewModel.setPin(pin)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showThemeDialog() {
        val options = arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system))
        val modes = arrayOf(AppThemeMode.LIGHT, AppThemeMode.DARK, AppThemeMode.SYSTEM)
        val current = modes.indexOf(viewModel.settings.value.themeMode).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_row_theme)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setThemeMode(modes[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showDistanceUnitDialog() {
        val options = arrayOf(getString(R.string.unit_km_full), getString(R.string.unit_miles_full))
        val units = arrayOf(DistanceUnit.KM, DistanceUnit.MILES)
        val current = units.indexOf(viewModel.settings.value.distanceUnit).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_row_distance_unit)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setDistanceUnit(units[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showFuelUnitDialog() {
        val options = arrayOf(getString(R.string.unit_l100km_full), getString(R.string.unit_mpg_full))
        val units = arrayOf(FuelEconomyUnit.L_100KM, FuelEconomyUnit.MPG)
        val current = units.indexOf(viewModel.settings.value.fuelEconomyUnit).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_row_fuel_unit)
            .setSingleChoiceItems(options, current) { dialog, which ->
                viewModel.setFuelEconomyUnit(units[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showCurrencyDialog() {
        val currencies = AppCurrency.values()
        val options = currencies.map { if (it == AppCurrency.CUSTOM) getString(R.string.currency_custom) else it.code }.toTypedArray()
        val current = currencies.indexOf(viewModel.settings.value.currency).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_row_currency)
            .setSingleChoiceItems(options, current) { dialog, which ->
                val chosen = currencies[which]
                if (chosen == AppCurrency.CUSTOM) {
                    dialog.dismiss()
                    showCustomCurrencyDialog()
                } else {
                    viewModel.setCurrency(chosen, null)
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun showCustomCurrencyDialog() {
        val input = EditText(requireContext()).apply { hint = getString(R.string.currency_custom_hint) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.currency_custom)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.setCurrency(AppCurrency.CUSTOM, input.text.toString().trim().uppercase().take(6))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = AppLanguage.values()
        val options = languages.map { languageLabel(it) }.toTypedArray()
        val current = languages.indexOf(viewModel.settings.value.language).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_row_language)
            .setSingleChoiceItems(options, current) { dialog, which ->
                val chosen = languages[which]
                viewModel.setLanguage(chosen)
                if (AppCompatDelegate.getApplicationLocales().get(0)?.language != chosen.tag) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(chosen.tag))
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showTextDialog(titleRes: Int, bodyRes: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(bodyRes)
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    private fun setupRow(
        row: ItemSettingsRowBinding,
        title: String,
        subtitle: String,
        showSwitch: Boolean,
        showChevron: Boolean = true,
        onClick: (() -> Unit)?
    ) {
        row.rowTitle.text = title
        row.rowSubtitle.text = subtitle
        row.rowSubtitle.visibility = if (subtitle.isBlank()) View.GONE else View.VISIBLE
        row.rowSwitch.visibility = if (showSwitch) View.VISIBLE else View.GONE
        row.rowChevron.visibility = if (showChevron && !showSwitch) View.VISIBLE else View.GONE
        if (onClick != null) row.root.setOnClickListener { onClick() } else row.root.isClickable = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
