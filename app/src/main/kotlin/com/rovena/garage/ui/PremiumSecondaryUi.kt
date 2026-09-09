package com.rovena.garage.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Troubleshoot
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rovena.garage.R
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.CarSymptom
import com.rovena.garage.data.DashboardAnalytics
import com.rovena.garage.data.FuelAnalytics
import com.rovena.garage.data.MaintenancePlanEngine
import com.rovena.garage.data.MaintenancePlanItem
import com.rovena.garage.data.MaintenancePlanStatus
import com.rovena.garage.data.MaintenanceTask
import com.rovena.garage.data.SymptomCatalog
import com.rovena.garage.data.SymptomUrgency
import com.rovena.garage.data.UpcomingUrgency
import com.rovena.garage.data.VehicleHealthEngine
import com.rovena.garage.data.WarningLightCatalog
import com.rovena.garage.data.WarningLightType
import com.rovena.garage.data.WarningSeverity
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import com.rovena.garage.ui.theme.RovenaPalette
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

private data class PremiumTimelineItem(
    val timestamp: Long,
    val action: RecordAction,
    val title: String,
    val detail: String,
    val amount: Double = 0.0
)

@Composable
internal fun PremiumHistoryScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier,
    onAddRecord: (RecordAction) -> Unit
) {
    val timeline = remember(maintenance, fuel, expenses, documents) {
        buildPremiumTimeline(maintenance, fuel, expenses, documents)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumSectionHeader(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.history),
                subtitle = stringResource(R.string.history_subtitle),
                trailing = timeline.size.takeIf { it > 0 }?.toString()
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { PremiumQuickChip(Icons.Rounded.Build, stringResource(R.string.maintenance)) { onAddRecord(RecordAction.MAINTENANCE) } }
                item { PremiumQuickChip(Icons.Rounded.LocalGasStation, stringResource(R.string.fuel)) { onAddRecord(RecordAction.FUEL) } }
                item { PremiumQuickChip(Icons.Rounded.ReceiptLong, stringResource(R.string.expenses)) { onAddRecord(RecordAction.EXPENSE) } }
                item { PremiumQuickChip(Icons.Rounded.Description, stringResource(R.string.documents)) { onAddRecord(RecordAction.DOCUMENT) } }
            }
        }

        when {
            vehicle == null -> item { PremiumEmptyCard(stringResource(R.string.no_vehicle_title), stringResource(R.string.no_vehicle_body)) }
            timeline.isEmpty() -> item { PremiumEmptyCard(stringResource(R.string.no_history_title), stringResource(R.string.no_history_body)) }
            else -> items(timeline, key = { "${it.action}-${it.timestamp}-${it.title}" }) { item ->
                PremiumTimelineCard(item, vehicle.currencyCode)
            }
        }
    }
}

@Composable
internal fun PremiumExpensesScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    modifier: Modifier = Modifier,
    onAddExpense: () -> Unit
) {
    val analytics = remember(maintenance, fuel, expenses) {
        DashboardAnalytics.monthlyCosts(maintenance, fuel, expenses)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.p5b_costs_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.p5b_costs_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = RovenaPalette.Accent) {
                    IconButton(onClick = onAddExpense) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.add_expense), tint = Color.White)
                    }
                }
            }
        }

        if (vehicle == null) {
            item { PremiumEmptyCard(stringResource(R.string.no_vehicle_title), stringResource(R.string.no_vehicle_body)) }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.38f))
                ) {
                    Box(
                        Modifier.fillMaxWidth().then(
                            Modifier
                        )
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(stringResource(R.string.p5b_this_month), color = RovenaPalette.TextSecondary, fontWeight = FontWeight.SemiBold)
                            Text(
                                premiumMoney(analytics.current.total, vehicle.currencyCode),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                stringResource(R.string.p5b_previous_month_value, premiumMoney(analytics.previous.total, vehicle.currencyCode)),
                                color = RovenaPalette.TextSecondary
                            )
                            Text(
                                premiumCostChange(analytics.changePercent),
                                color = when {
                                    analytics.changePercent == null -> RovenaPalette.TextSecondary
                                    analytics.changePercent <= 0.0 -> RovenaPalette.Success
                                    else -> RovenaPalette.Warning
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumCostTile(Modifier.weight(1f), Icons.Rounded.LocalGasStation, stringResource(R.string.fuel), analytics.current.fuel, vehicle.currencyCode, RovenaPalette.Cyan)
                    PremiumCostTile(Modifier.weight(1f), Icons.Rounded.Build, stringResource(R.string.maintenance), analytics.current.maintenance, vehicle.currencyCode, RovenaPalette.Accent)
                    PremiumCostTile(Modifier.weight(1f), Icons.Rounded.ReceiptLong, stringResource(R.string.p5b_other), analytics.current.other, vehicle.currencyCode, RovenaPalette.Warning)
                }
            }

            item {
                PremiumInfoRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.p5b_cost_100km),
                    subtitle = analytics.distanceKm?.let { stringResource(R.string.p5b_distance_basis, NumberFormat.getIntegerInstance().format(it)) }
                        ?: stringResource(R.string.p5b_distance_not_enough),
                    value = analytics.costPer100Km?.let { premiumMoney(it, vehicle.currencyCode) } ?: "—"
                )
            }

            item {
                Text(stringResource(R.string.p5b_top_categories), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            if (analytics.topOtherCategories.isEmpty()) {
                item { PremiumEmptyCard(stringResource(R.string.p5b_no_categories), stringResource(R.string.p5b_no_categories_body)) }
            } else {
                val max = analytics.topOtherCategories.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
                items(analytics.topOtherCategories, key = { it.first }) { (category, amount) ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.7f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Payments, null, tint = RovenaPalette.Cyan)
                                Text(category, modifier = Modifier.weight(1f).padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                                Text(premiumMoney(amount, vehicle.currencyCode), fontWeight = FontWeight.Black)
                            }
                            LinearProgressIndicator(
                                progress = { (amount / max).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = RovenaPalette.Cyan,
                                trackColor = RovenaPalette.SurfaceSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumSmartCenterScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    preferences: AppPreferences,
    selectedLanguage: String,
    modifier: Modifier = Modifier,
    onAddDocument: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShareReport: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
            PremiumSectionHeader(
                icon = Icons.Rounded.HealthAndSafety,
                title = stringResource(R.string.insights),
                subtitle = "Rovena Auto"
            )
            Spacer(Modifier.height(12.dp))
            PremiumSegmentedTabs(page = page, onPage = { page = it })
        }

        Box(Modifier.weight(1f)) {
            when (page) {
                0 -> PremiumSmartOverview(
                    vehicle = vehicle,
                    maintenance = maintenance,
                    fuel = fuel,
                    expenses = expenses,
                    documents = documents,
                    onAddDocument = onAddDocument,
                    onShareReport = onShareReport,
                    onExportBackup = onExportBackup
                )
                1 -> PremiumVehicleToolsScreen(vehicle, maintenance)
                else -> PremiumSettingsScreen(
                    preferences = preferences,
                    selectedLanguage = selectedLanguage,
                    reportEnabled = vehicle != null,
                    maintenance = maintenance,
                    fuel = fuel,
                    expenses = expenses,
                    documents = documents,
                    onLanguageSelected = onLanguageSelected,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onShareReport = onShareReport
                )
            }
        }
    }
}

@Composable
private fun PremiumSmartOverview(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    onAddDocument: () -> Unit,
    onShareReport: () -> Unit,
    onExportBackup: () -> Unit
) {
    val health = remember(vehicle, maintenance, documents, fuel) {
        vehicle?.let { VehicleHealthEngine.evaluate(it, maintenance, documents, fuel) }
    }
    val costs = remember(maintenance, fuel, expenses) { DashboardAnalytics.monthlyCosts(maintenance, fuel, expenses) }
    val fuelInsights = remember(fuel) { FuelAnalytics.analyze(fuel) }
    val upcoming = remember(vehicle, maintenance, documents) {
        vehicle?.let { DashboardAnalytics.upcoming(it, maintenance, documents) }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (vehicle == null) {
            item { PremiumEmptyCard(stringResource(R.string.no_vehicle_title), stringResource(R.string.no_vehicle_body)) }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.45f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = RovenaPalette.Accent.copy(alpha = 0.16f)) {
                            Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                                Text(health?.score?.toString() ?: "—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = RovenaPalette.Cyan)
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(vehicleDisplayName(vehicle), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(stringResource(R.string.p5b_care_score_short), color = RovenaPalette.TextSecondary)
                            Text(
                                if ((health?.attentionCount ?: 0) > 0) stringResource(R.string.p5b_attention_items, health?.attentionCount ?: 0)
                                else stringResource(R.string.p5b_no_attention),
                                color = if ((health?.attentionCount ?: 0) > 0) RovenaPalette.Warning else RovenaPalette.Success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSmartMetric(Modifier.weight(1f), Icons.Rounded.Payments, stringResource(R.string.p5b_this_month), premiumMoney(costs.current.total, vehicle.currencyCode))
                    PremiumSmartMetric(Modifier.weight(1f), Icons.Rounded.LocalGasStation, stringResource(R.string.p5b_fuel_efficiency), fuelInsights.averageKmPerLiter?.let { String.format(Locale.getDefault(), "%.1f km/L", it) } ?: "—")
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSmartMetric(Modifier.weight(1f), Icons.Rounded.Description, stringResource(R.string.documents), documents.size.toString())
                    PremiumSmartMetric(Modifier.weight(1f), Icons.Rounded.Warning, stringResource(R.string.p5b_next_for_car), upcoming.count { it.urgency != UpcomingUrgency.LATER }.toString())
                }
            }
        }

        item {
            Text(stringResource(R.string.data_tools_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.data_tools_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumActionTile(Modifier.weight(1f), Icons.Rounded.Description, stringResource(R.string.add_document), onAddDocument)
                PremiumActionTile(Modifier.weight(1f), Icons.Rounded.PictureAsPdf, stringResource(R.string.share_vehicle_report), onShareReport)
            }
        }
        item {
            PremiumActionTile(Modifier.fillMaxWidth(), Icons.Rounded.Backup, stringResource(R.string.export_backup), onExportBackup)
        }
    }
}

@Composable
private fun PremiumVehicleToolsScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>
) {
    var expandedWarning by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSymptom by rememberSaveable { mutableStateOf<String?>(null) }
    val plan = remember(vehicle, maintenance) {
        vehicle?.let { MaintenancePlanEngine.suggestions(it, maintenance) }.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.car_tools_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.car_tools_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item { PremiumToolHeader(Icons.Rounded.Build, stringResource(R.string.maintenance_plan_title), stringResource(R.string.maintenance_plan_subtitle)) }
        if (vehicle == null) {
            item { PremiumEmptyCard(stringResource(R.string.no_vehicle_title), stringResource(R.string.no_vehicle_body)) }
        } else {
            items(plan.take(7), key = { it.task.name }) { item -> PremiumMaintenancePlanCard(item) }
        }

        item { PremiumToolHeader(Icons.Rounded.Warning, stringResource(R.string.warning_lights_title), stringResource(R.string.warning_lights_subtitle)) }
        items(WarningLightCatalog.all, key = { it.type.name }) { guide ->
            val expanded = expandedWarning == guide.type.name
            PremiumExpandableToolCard(
                icon = Icons.Rounded.Warning,
                title = premiumWarningTitle(guide.type),
                status = premiumWarningSeverityLabel(guide.severity),
                statusColor = premiumWarningSeverityColor(guide.severity),
                detail = premiumWarningDetail(guide.type),
                expanded = expanded,
                onClick = { expandedWarning = if (expanded) null else guide.type.name }
            )
        }

        item { PremiumToolHeader(Icons.Rounded.Troubleshoot, stringResource(R.string.symptoms_title), stringResource(R.string.symptoms_subtitle)) }
        items(SymptomCatalog.all, key = { it.symptom.name }) { guide ->
            val expanded = expandedSymptom == guide.symptom.name
            PremiumExpandableToolCard(
                icon = Icons.Rounded.Troubleshoot,
                title = premiumSymptomTitle(guide.symptom),
                status = premiumSymptomUrgencyLabel(guide.urgency),
                statusColor = premiumSymptomUrgencyColor(guide.urgency),
                detail = premiumSymptomDetail(guide.symptom),
                expanded = expanded,
                onClick = { expandedSymptom = if (expanded) null else guide.symptom.name }
            )
        }

        item {
            PremiumEmptyCard(stringResource(R.string.guidance_disclaimer_title), stringResource(R.string.guidance_disclaimer))
        }
    }
}

@Composable
private fun PremiumSettingsScreen(
    preferences: AppPreferences,
    selectedLanguage: String,
    reportEnabled: Boolean,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    onLanguageSelected: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShareReport: () -> Unit
) {
    val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = AppPreferences.THEME_DARK)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val attachments = remember(maintenance, fuel, expenses, documents) {
        buildList {
            maintenance.forEach { it.attachmentUri?.let { uri -> add(it.serviceType to uri) } }
            fuel.forEach { it.attachmentUri?.let { uri -> add("${it.liters} L" to uri) } }
            expenses.forEach { it.attachmentUri?.let { uri -> add(it.category to uri) } }
            documents.forEach { it.fileUri?.let { uri -> add(it.title to uri) } }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.settings_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            PremiumSettingsCard(Icons.Rounded.DarkMode, stringResource(R.string.appearance_title), stringResource(R.string.appearance_subtitle)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { PremiumThemeChip(AppPreferences.THEME_SYSTEM, themeMode, stringResource(R.string.theme_system)) { scope.launch { preferences.setThemeMode(AppPreferences.THEME_SYSTEM) } } }
                    item { PremiumThemeChip(AppPreferences.THEME_LIGHT, themeMode, stringResource(R.string.theme_light)) { scope.launch { preferences.setThemeMode(AppPreferences.THEME_LIGHT) } } }
                    item { PremiumThemeChip(AppPreferences.THEME_DARK, themeMode, stringResource(R.string.theme_dark)) { scope.launch { preferences.setThemeMode(AppPreferences.THEME_DARK) } } }
                }
            }
        }

        item {
            PremiumSettingsCard(Icons.Rounded.Language, stringResource(R.string.language_title), stringResource(R.string.language_subtitle)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ar" to "العربية", "en" to "English", "fr" to "Français", "es" to "Español", "de" to "Deutsch", "tr" to "Türkçe").forEach { (tag, label) ->
                        item {
                            FilterChip(selected = selectedLanguage == tag, onClick = { onLanguageSelected(tag) }, label = { Text(label) })
                        }
                    }
                }
            }
        }

        item {
            PremiumSettingsCard(Icons.Rounded.Notifications, stringResource(R.string.notification_fixed_title), stringResource(R.string.notification_fixed_every_12h)) {
                PremiumLockedSwitch(stringResource(R.string.vehicle_reminders_setting))
                PremiumLockedSwitch(stringResource(R.string.engagement_reminders_setting))
                Text(stringResource(R.string.notification_fixed_note), style = MaterialTheme.typography.bodySmall, color = RovenaPalette.TextSecondary)
            }
        }

        if (attachments.isNotEmpty()) {
            item {
                PremiumSettingsCard(Icons.Rounded.AttachFile, stringResource(R.string.attachments_title), stringResource(R.string.attachments_subtitle, attachments.size)) {
                    attachments.take(10).forEach { (title, uri) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { premiumOpenAttachment(context, uri) },
                            shape = RoundedCornerShape(16.dp),
                            color = RovenaPalette.SurfaceRaised,
                            border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.65f))
                        ) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AttachFile, null, tint = RovenaPalette.Cyan)
                                Text(title, Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Rounded.OpenInNew, null, tint = RovenaPalette.TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        item {
            PremiumSettingsCard(Icons.Rounded.Backup, stringResource(R.string.data_tools_title), stringResource(R.string.data_tools_subtitle)) {
                Button(onClick = onShareReport, enabled = reportEnabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PictureAsPdf, null)
                    Text("  ${stringResource(R.string.share_vehicle_report)}")
                }
                PremiumSettingsAction(Icons.Rounded.Backup, stringResource(R.string.export_backup), onExportBackup)
                PremiumSettingsAction(Icons.Rounded.Restore, stringResource(R.string.import_backup), onImportBackup)
                Text(stringResource(R.string.backup_attachment_note), style = MaterialTheme.typography.bodySmall, color = RovenaPalette.TextSecondary)
            }
        }
    }
}

@Composable
private fun PremiumSegmentedTabs(page: Int, onPage: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RovenaPalette.Surface,
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.75f))
    ) {
        Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(
                stringResource(R.string.smart_tab_overview),
                stringResource(R.string.smart_tab_tools),
                stringResource(R.string.smart_tab_settings)
            ).forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier.weight(1f).clickable { onPage(index) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (page == index) RovenaPalette.Accent else Color.Transparent
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 11.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (page == index) Color.White else RovenaPalette.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSectionHeader(icon: ImageVector, title: String, subtitle: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(16.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
            Icon(icon, null, modifier = Modifier.padding(11.dp), tint = RovenaPalette.Cyan)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.let {
            Surface(shape = RoundedCornerShape(99.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                Text(it, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = RovenaPalette.Cyan, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PremiumQuickChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.75f))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = RovenaPalette.Cyan)
            Text("  $label", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PremiumTimelineCard(item: PremiumTimelineItem, currencyCode: String) {
    val (icon, color) = when (item.action) {
        RecordAction.MAINTENANCE -> Icons.Rounded.Build to RovenaPalette.Accent
        RecordAction.FUEL -> Icons.Rounded.LocalGasStation to RovenaPalette.Cyan
        RecordAction.EXPENSE -> Icons.Rounded.ReceiptLong to RovenaPalette.Warning
        RecordAction.DOCUMENT -> Icons.Rounded.Description to RovenaPalette.Sky
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = color.copy(alpha = 0.13f)) {
                Icon(icon, null, modifier = Modifier.padding(11.dp), tint = color)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.detail, color = RovenaPalette.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(premiumDate(item.timestamp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            if (item.amount > 0.0) Text(premiumMoney(item.amount, currencyCode), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremiumCostTile(modifier: Modifier, icon: ImageVector, label: String, amount: Double, currencyCode: String, accent: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = RovenaPalette.TextSecondary, maxLines = 1)
            Text(premiumMoney(amount, currencyCode), fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun PremiumInfoRow(icon: ImageVector, title: String, subtitle: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                Icon(icon, null, modifier = Modifier.padding(10.dp), tint = RovenaPalette.Cyan)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RovenaPalette.TextSecondary)
            }
            Text(value, fontWeight = FontWeight.Black, color = RovenaPalette.Cyan)
        }
    }
}

@Composable
private fun PremiumSmartMetric(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = RovenaPalette.Cyan)
            Text(label, style = MaterialTheme.typography.labelMedium, color = RovenaPalette.TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremiumActionTile(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = RovenaPalette.Accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.34f))
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RovenaPalette.Cyan)
            Text("  $label", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PremiumToolHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(16.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
            Icon(icon, null, modifier = Modifier.padding(11.dp), tint = RovenaPalette.Cyan)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = RovenaPalette.TextSecondary)
        }
    }
}

@Composable
private fun PremiumMaintenancePlanCard(item: MaintenancePlanItem) {
    val statusColor = when (item.status) {
        MaintenancePlanStatus.OVERDUE -> RovenaPalette.Danger
        MaintenancePlanStatus.DUE_SOON -> RovenaPalette.Warning
        MaintenancePlanStatus.ON_TRACK -> RovenaPalette.Success
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = statusColor.copy(alpha = 0.13f)) {
                Icon(Icons.Rounded.Build, null, modifier = Modifier.padding(10.dp), tint = statusColor)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(premiumMaintenanceTaskLabel(item.task), fontWeight = FontWeight.Black)
                Text(
                    stringResource(R.string.maintenance_every_km_months, NumberFormat.getIntegerInstance().format(item.intervalKm), item.intervalMonths),
                    style = MaterialTheme.typography.bodySmall,
                    color = RovenaPalette.TextSecondary
                )
                Text(premiumMaintenanceDueText(item), style = MaterialTheme.typography.bodySmall)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = statusColor.copy(alpha = 0.13f)) {
                Text(premiumMaintenanceStatusLabel(item.status), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PremiumExpandableToolCard(
    icon: ImageVector,
    title: String,
    status: String,
    statusColor: Color,
    detail: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Icon(icon, null, modifier = Modifier.padding(9.dp), tint = statusColor)
                }
                Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Text(title, fontWeight = FontWeight.Black)
                    Text(status, color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text(if (expanded) "−" else "+", color = RovenaPalette.Cyan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            if (expanded) Text(detail, color = RovenaPalette.TextSecondary)
            else Text(stringResource(R.string.tap_for_details), color = RovenaPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PremiumSettingsCard(icon: ImageVector, title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                    Icon(icon, null, modifier = Modifier.padding(10.dp), tint = RovenaPalette.Cyan)
                }
                Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RovenaPalette.TextSecondary)
                }
            }
            content()
        }
    }
}

@Composable
private fun PremiumThemeChip(mode: String, current: String, label: String, onClick: () -> Unit) {
    FilterChip(selected = current == mode, onClick = onClick, label = { Text(label) })
}

@Composable
private fun PremiumLockedSwitch(title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(checked = true, onCheckedChange = null, enabled = false)
    }
}

@Composable
private fun PremiumSettingsAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = RovenaPalette.Accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.3f))
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RovenaPalette.Cyan)
            Text("  $label", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PremiumEmptyCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.72f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(body, color = RovenaPalette.TextSecondary)
        }
    }
}

private fun buildPremiumTimeline(
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>
): List<PremiumTimelineItem> = buildList {
    maintenance.forEach {
        add(PremiumTimelineItem(it.performedAt, RecordAction.MAINTENANCE, it.serviceType, NumberFormat.getIntegerInstance().format(it.mileage) + " km", it.cost))
    }
    fuel.forEach {
        add(PremiumTimelineItem(it.filledAt, RecordAction.FUEL, "${it.liters} L", NumberFormat.getIntegerInstance().format(it.mileage) + " km", it.totalCost))
    }
    expenses.forEach {
        add(PremiumTimelineItem(it.spentAt, RecordAction.EXPENSE, it.category, it.notes.ifBlank { it.category }, it.amount))
    }
    documents.forEach {
        add(PremiumTimelineItem(it.createdAt, RecordAction.DOCUMENT, it.title, it.category, 0.0))
    }
}.sortedByDescending { it.timestamp }

private fun premiumDate(timestamp: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

private fun premiumMoney(amount: Double, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(amount)
}.getOrElse { String.format(Locale.getDefault(), "%.2f %s", amount, currencyCode) }

private fun premiumCostChange(percent: Double?): String = when {
    percent == null -> "—"
    percent == 0.0 -> "0%"
    else -> String.format(Locale.getDefault(), "%+.1f%%", percent)
}

@Composable
private fun premiumMaintenanceTaskLabel(task: MaintenanceTask): String = stringResource(
    when (task) {
        MaintenanceTask.ENGINE_OIL -> R.string.task_engine_oil
        MaintenanceTask.AIR_FILTER -> R.string.task_air_filter
        MaintenanceTask.CABIN_FILTER -> R.string.task_cabin_filter
        MaintenanceTask.BRAKE_INSPECTION -> R.string.task_brake_inspection
        MaintenanceTask.BRAKE_FLUID -> R.string.task_brake_fluid
        MaintenanceTask.COOLANT -> R.string.task_coolant
        MaintenanceTask.TRANSMISSION_FLUID -> R.string.task_transmission_fluid
        MaintenanceTask.SPARK_PLUGS -> R.string.task_spark_plugs
        MaintenanceTask.TIRE_ROTATION -> R.string.task_tire_rotation
    }
)

@Composable
private fun premiumMaintenanceStatusLabel(status: MaintenancePlanStatus): String = stringResource(
    when (status) {
        MaintenancePlanStatus.OVERDUE -> R.string.maintenance_status_overdue
        MaintenancePlanStatus.DUE_SOON -> R.string.maintenance_status_due_soon
        MaintenancePlanStatus.ON_TRACK -> R.string.maintenance_status_on_track
    }
)

@Composable
private fun premiumMaintenanceDueText(item: MaintenancePlanItem): String = when {
    item.remainingKm < 0 -> stringResource(R.string.maintenance_overdue_km, NumberFormat.getIntegerInstance().format(-item.remainingKm))
    item.remainingKm == 0L -> stringResource(R.string.maintenance_due_now)
    else -> stringResource(R.string.maintenance_due_in_km, NumberFormat.getIntegerInstance().format(item.remainingKm))
}

@Composable
private fun premiumWarningTitle(type: WarningLightType): String = stringResource(
    when (type) {
        WarningLightType.OIL_PRESSURE -> R.string.warning_oil_title
        WarningLightType.COOLANT_TEMPERATURE -> R.string.warning_temp_title
        WarningLightType.BRAKE_SYSTEM -> R.string.warning_brake_title
        WarningLightType.BATTERY_CHARGING -> R.string.warning_battery_title
        WarningLightType.CHECK_ENGINE -> R.string.warning_engine_title
        WarningLightType.ABS -> R.string.warning_abs_title
    }
)

@Composable
private fun premiumWarningDetail(type: WarningLightType): String = stringResource(
    when (type) {
        WarningLightType.OIL_PRESSURE -> R.string.warning_oil_detail
        WarningLightType.COOLANT_TEMPERATURE -> R.string.warning_temp_detail
        WarningLightType.BRAKE_SYSTEM -> R.string.warning_brake_detail
        WarningLightType.BATTERY_CHARGING -> R.string.warning_battery_detail
        WarningLightType.CHECK_ENGINE -> R.string.warning_engine_detail
        WarningLightType.ABS -> R.string.warning_abs_detail
    }
)

@Composable
private fun premiumWarningSeverityLabel(severity: WarningSeverity): String = stringResource(
    when (severity) {
        WarningSeverity.INFO -> R.string.severity_info
        WarningSeverity.CAUTION -> R.string.severity_caution
        WarningSeverity.STOP_SAFELY -> R.string.severity_stop
    }
)

private fun premiumWarningSeverityColor(severity: WarningSeverity): Color = when (severity) {
    WarningSeverity.STOP_SAFELY -> RovenaPalette.Danger
    WarningSeverity.CAUTION -> RovenaPalette.Warning
    WarningSeverity.INFO -> RovenaPalette.Cyan
}

@Composable
private fun premiumSymptomTitle(symptom: CarSymptom): String = stringResource(
    when (symptom) {
        CarSymptom.OVERHEATING -> R.string.symptom_overheat_title
        CarSymptom.SHAKING_AT_IDLE -> R.string.symptom_shaking_title
        CarSymptom.BRAKE_NOISE -> R.string.symptom_brake_noise_title
        CarSymptom.STEERING_NOISE -> R.string.symptom_steering_title
        CarSymptom.HIGH_FUEL_USE -> R.string.symptom_fuel_title
    }
)

@Composable
private fun premiumSymptomDetail(symptom: CarSymptom): String = stringResource(
    when (symptom) {
        CarSymptom.OVERHEATING -> R.string.symptom_overheat_detail
        CarSymptom.SHAKING_AT_IDLE -> R.string.symptom_shaking_detail
        CarSymptom.BRAKE_NOISE -> R.string.symptom_brake_noise_detail
        CarSymptom.STEERING_NOISE -> R.string.symptom_steering_detail
        CarSymptom.HIGH_FUEL_USE -> R.string.symptom_fuel_detail
    }
)

@Composable
private fun premiumSymptomUrgencyLabel(urgency: SymptomUrgency): String = stringResource(
    when (urgency) {
        SymptomUrgency.NORMAL -> R.string.urgency_normal
        SymptomUrgency.SOON -> R.string.urgency_soon
        SymptomUrgency.URGENT -> R.string.urgency_urgent
    }
)

private fun premiumSymptomUrgencyColor(urgency: SymptomUrgency): Color = when (urgency) {
    SymptomUrgency.URGENT -> RovenaPalette.Danger
    SymptomUrgency.SOON -> RovenaPalette.Warning
    SymptomUrgency.NORMAL -> RovenaPalette.Success
}

private fun premiumOpenAttachment(context: android.content.Context, rawUri: String) {
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
