package com.rovena.garage.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Troubleshoot
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.CarSymptom
import com.rovena.garage.data.MaintenancePlanEngine
import com.rovena.garage.data.MaintenancePlanItem
import com.rovena.garage.data.MaintenancePlanStatus
import com.rovena.garage.data.MaintenanceTask
import com.rovena.garage.data.SymptomCatalog
import com.rovena.garage.data.SymptomUrgency
import com.rovena.garage.data.WarningLightCatalog
import com.rovena.garage.data.WarningLightType
import com.rovena.garage.data.WarningSeverity
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.text.NumberFormat

@Composable
internal fun SmartCenterScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier,
    onAddDocument: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = page == 0,
                onClick = { page = 0 },
                label = { Text(stringResource(R.string.smart_tab_overview)) }
            )
            FilterChip(
                selected = page == 1,
                onClick = { page = 1 },
                label = { Text(stringResource(R.string.smart_tab_tools)) }
            )
        }
        Box(Modifier.weight(1f)) {
            if (page == 0) {
                SmartHubScreen(
                    vehicle = vehicle,
                    maintenance = maintenance,
                    fuel = fuel,
                    documents = documents,
                    modifier = Modifier.fillMaxSize(),
                    onAddDocument = onAddDocument
                )
            } else {
                VehicleToolsScreen(
                    vehicle = vehicle,
                    maintenance = maintenance,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VehicleToolsScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    modifier: Modifier = Modifier
) {
    var expandedWarning by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSymptom by rememberSaveable { mutableStateOf<String?>(null) }
    val plan = remember(vehicle, maintenance) {
        vehicle?.let { MaintenancePlanEngine.suggestions(it, maintenance) }.orEmpty()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.car_tools_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.car_tools_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (vehicle == null) {
            item {
                Stage4InfoCard(R.string.no_vehicle_title, R.string.no_vehicle_body)
            }
        } else {
            item {
                SectionHeader(
                    icon = { Icon(Icons.Rounded.Build, null) },
                    title = stringResource(R.string.maintenance_plan_title),
                    subtitle = stringResource(R.string.maintenance_plan_subtitle)
                )
            }
            items(plan.take(7), key = { it.task.name }) { item ->
                MaintenancePlanCard(item)
            }
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Rounded.Warning, null) },
                title = stringResource(R.string.warning_lights_title),
                subtitle = stringResource(R.string.warning_lights_subtitle)
            )
        }
        items(WarningLightCatalog.all, key = { it.type.name }) { guide ->
            val expanded = expandedWarning == guide.type.name
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    expandedWarning = if (expanded) null else guide.type.name
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(warningTitle(guide.type), fontWeight = FontWeight.Bold)
                            Text(warningSeverityLabel(guide.severity), color = warningSeverityColor(guide.severity), style = MaterialTheme.typography.labelLarge)
                        }
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    if (expanded) {
                        Text(warningDetail(guide.type), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.tap_for_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            SectionHeader(
                icon = { Icon(Icons.Rounded.Troubleshoot, null) },
                title = stringResource(R.string.symptoms_title),
                subtitle = stringResource(R.string.symptoms_subtitle)
            )
        }
        items(SymptomCatalog.all, key = { it.symptom.name }) { guide ->
            val expanded = expandedSymptom == guide.symptom.name
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    expandedSymptom = if (expanded) null else guide.symptom.name
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(symptomTitle(guide.symptom), fontWeight = FontWeight.Bold)
                            Text(symptomUrgencyLabel(guide.urgency), color = symptomUrgencyColor(guide.urgency), style = MaterialTheme.typography.labelLarge)
                        }
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    if (expanded) {
                        Text(symptomDetail(guide.symptom), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.tap_for_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Stage4InfoCard(R.string.guidance_disclaimer_title, R.string.guidance_disclaimer)
        }
    }
}

@Composable
private fun MaintenancePlanCard(item: MaintenancePlanItem) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(maintenanceTaskLabel(item.task), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(
                        R.string.maintenance_every_km_months,
                        NumberFormat.getIntegerInstance().format(item.intervalKm),
                        item.intervalMonths
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(maintenanceDueText(item), style = MaterialTheme.typography.bodyMedium)
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = when (item.status) {
                    MaintenancePlanStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
                    MaintenancePlanStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer
                    MaintenancePlanStatus.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    maintenanceStatusLabel(item.status),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun maintenanceDueText(item: MaintenancePlanItem): String = when {
    item.remainingKm < 0 -> stringResource(R.string.maintenance_overdue_km, NumberFormat.getIntegerInstance().format(-item.remainingKm))
    item.remainingKm == 0L -> stringResource(R.string.maintenance_due_now)
    else -> stringResource(R.string.maintenance_due_in_km, NumberFormat.getIntegerInstance().format(item.remainingKm))
}

@Composable
private fun SectionHeader(icon: @Composable () -> Unit, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.padding(10.dp)) { icon() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stage4InfoCard(title: Int, body: Int) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.Bold)
            Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun maintenanceTaskLabel(task: MaintenanceTask): String = stringResource(
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
private fun maintenanceStatusLabel(status: MaintenancePlanStatus): String = stringResource(
    when (status) {
        MaintenancePlanStatus.OVERDUE -> R.string.maintenance_status_overdue
        MaintenancePlanStatus.DUE_SOON -> R.string.maintenance_status_due_soon
        MaintenancePlanStatus.ON_TRACK -> R.string.maintenance_status_on_track
    }
)

@Composable
private fun warningTitle(type: WarningLightType): String = stringResource(
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
private fun warningDetail(type: WarningLightType): String = stringResource(
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
private fun warningSeverityLabel(severity: WarningSeverity): String = stringResource(
    when (severity) {
        WarningSeverity.INFO -> R.string.severity_info
        WarningSeverity.CAUTION -> R.string.severity_caution
        WarningSeverity.STOP_SAFELY -> R.string.severity_stop
    }
)

@Composable
private fun warningSeverityColor(severity: WarningSeverity) = when (severity) {
    WarningSeverity.STOP_SAFELY -> MaterialTheme.colorScheme.error
    WarningSeverity.CAUTION -> MaterialTheme.colorScheme.tertiary
    WarningSeverity.INFO -> MaterialTheme.colorScheme.primary
}

@Composable
private fun symptomTitle(symptom: CarSymptom): String = stringResource(
    when (symptom) {
        CarSymptom.OVERHEATING -> R.string.symptom_overheat_title
        CarSymptom.SHAKING_AT_IDLE -> R.string.symptom_shaking_title
        CarSymptom.BRAKE_NOISE -> R.string.symptom_brake_noise_title
        CarSymptom.STEERING_NOISE -> R.string.symptom_steering_title
        CarSymptom.HIGH_FUEL_USE -> R.string.symptom_fuel_title
    }
)

@Composable
private fun symptomDetail(symptom: CarSymptom): String = stringResource(
    when (symptom) {
        CarSymptom.OVERHEATING -> R.string.symptom_overheat_detail
        CarSymptom.SHAKING_AT_IDLE -> R.string.symptom_shaking_detail
        CarSymptom.BRAKE_NOISE -> R.string.symptom_brake_noise_detail
        CarSymptom.STEERING_NOISE -> R.string.symptom_steering_detail
        CarSymptom.HIGH_FUEL_USE -> R.string.symptom_fuel_detail
    }
)

@Composable
private fun symptomUrgencyLabel(urgency: SymptomUrgency): String = stringResource(
    when (urgency) {
        SymptomUrgency.NORMAL -> R.string.urgency_normal
        SymptomUrgency.SOON -> R.string.urgency_soon
        SymptomUrgency.URGENT -> R.string.urgency_urgent
    }
)

@Composable
private fun symptomUrgencyColor(urgency: SymptomUrgency) = when (urgency) {
    SymptomUrgency.URGENT -> MaterialTheme.colorScheme.error
    SymptomUrgency.SOON -> MaterialTheme.colorScheme.tertiary
    SymptomUrgency.NORMAL -> MaterialTheme.colorScheme.primary
}
