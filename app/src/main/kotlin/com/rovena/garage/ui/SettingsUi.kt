package com.rovena.garage.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rovena.garage.R
import com.rovena.garage.RovenaApp
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal data class AttachmentEntry(val title: String, val uri: String)

@Composable
internal fun SettingsScreen(
    preferences: AppPreferences,
    selectedLanguage: String,
    reportEnabled: Boolean,
    modifier: Modifier = Modifier,
    onLanguageSelected: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShareReport: () -> Unit
) {
    val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = AppPreferences.THEME_SYSTEM)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as RovenaApp
    val vehicles by app.vehicleRepository.vehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentVehicle = vehicles.firstOrNull { it.isPrimary } ?: vehicles.firstOrNull()
    val currentId = currentVehicle?.id
    val maintenanceFlow = remember(currentId) {
        currentId?.let(app.recordRepository::maintenance) ?: flowOf(emptyList<MaintenanceEntity>())
    }
    val fuelFlow = remember(currentId) {
        currentId?.let(app.recordRepository::fuel) ?: flowOf(emptyList<FuelEntryEntity>())
    }
    val expenseFlow = remember(currentId) {
        currentId?.let(app.recordRepository::expenses) ?: flowOf(emptyList<ExpenseEntity>())
    }
    val documentFlow = remember(currentId) {
        currentId?.let(app.recordRepository::documents) ?: flowOf(emptyList<DocumentEntity>())
    }
    val maintenance by maintenanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val fuel by fuelFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by expenseFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val documents by documentFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val attachments = remember(maintenance, fuel, expenses, documents) {
        buildList {
            maintenance.forEach { it.attachmentUri?.let { uri -> add(AttachmentEntry(it.serviceType, uri)) } }
            fuel.forEach { it.attachmentUri?.let { uri -> add(AttachmentEntry("${it.liters} L", uri)) } }
            expenses.forEach { it.attachmentUri?.let { uri -> add(AttachmentEntry(it.category, uri)) } }
            documents.forEach { it.fileUri?.let { uri -> add(AttachmentEntry(it.title, uri)) } }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.settings_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Rounded.DarkMode, null) },
                title = stringResource(R.string.appearance_title),
                subtitle = stringResource(R.string.appearance_subtitle)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ThemeChip(AppPreferences.THEME_SYSTEM, themeMode, R.string.theme_system) {
                            scope.launch { preferences.setThemeMode(AppPreferences.THEME_SYSTEM) }
                        }
                    }
                    item {
                        ThemeChip(AppPreferences.THEME_LIGHT, themeMode, R.string.theme_light) {
                            scope.launch { preferences.setThemeMode(AppPreferences.THEME_LIGHT) }
                        }
                    }
                    item {
                        ThemeChip(AppPreferences.THEME_DARK, themeMode, R.string.theme_dark) {
                            scope.launch { preferences.setThemeMode(AppPreferences.THEME_DARK) }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Rounded.Language, null) },
                title = stringResource(R.string.language_title),
                subtitle = stringResource(R.string.language_subtitle)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ar" to "العربية",
                        "en" to "English",
                        "fr" to "Français",
                        "es" to "Español",
                        "de" to "Deutsch",
                        "tr" to "Türkçe"
                    ).forEach { (tag, label) ->
                        item {
                            FilterChip(
                                selected = selectedLanguage == tag,
                                onClick = { onLanguageSelected(tag) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Rounded.Notifications, null) },
                title = stringResource(R.string.notification_fixed_title),
                subtitle = stringResource(R.string.notification_fixed_every_12h)
            ) {
                LockedNotificationRow(stringResource(R.string.vehicle_reminders_setting))
                LockedNotificationRow(stringResource(R.string.engagement_reminders_setting))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        stringResource(R.string.notification_fixed_every_12h),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    stringResource(R.string.notification_fixed_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (attachments.isNotEmpty()) {
            item {
                SettingsCard(
                    icon = { Icon(Icons.Rounded.AttachFile, null) },
                    title = stringResource(R.string.attachments_title),
                    subtitle = stringResource(R.string.attachments_subtitle, attachments.size)
                ) {
                    attachments.take(12).forEach { attachment ->
                        FilledTonalButton(
                            onClick = { openAttachment(context, attachment.uri) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.OpenInNew, null)
                            Text("  ${attachment.title}", maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = { Icon(Icons.Rounded.Backup, null) },
                title = stringResource(R.string.data_tools_title),
                subtitle = stringResource(R.string.data_tools_subtitle)
            ) {
                Button(onClick = onShareReport, enabled = reportEnabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PictureAsPdf, null)
                    Text("  ${stringResource(R.string.share_vehicle_report)}")
                }
                FilledTonalButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Backup, null)
                    Text("  ${stringResource(R.string.export_backup)}")
                }
                FilledTonalButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Restore, null)
                    Text("  ${stringResource(R.string.import_backup)}")
                }
                Text(
                    stringResource(R.string.backup_attachment_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun openAttachment(context: android.content.Context, rawUri: String) {
    runCatching {
        val uri = Uri.parse(rawUri)
        val mime = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_attachment)))
    }
}

@Composable
private fun ThemeChip(mode: String, current: String, label: Int, onClick: () -> Unit) {
    FilterChip(
        selected = current == mode,
        onClick = onClick,
        label = { Text(stringResource(label)) }
    )
}

@Composable
private fun SettingsCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                icon()
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun LockedNotificationRow(title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(checked = true, onCheckedChange = null, enabled = false)
    }
}
