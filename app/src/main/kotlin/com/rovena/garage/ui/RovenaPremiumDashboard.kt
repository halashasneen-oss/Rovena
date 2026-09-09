package com.rovena.garage.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.DashboardAnalytics
import com.rovena.garage.data.FuelAnalytics
import com.rovena.garage.data.UpcomingItem
import com.rovena.garage.data.UpcomingKind
import com.rovena.garage.data.UpcomingUrgency
import com.rovena.garage.data.VehicleHealthEngine
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import com.rovena.garage.ui.theme.RovenaPalette
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@Composable
internal fun PremiumDashboardScreen(
    vehicles: List<VehicleEntity>,
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit,
    onSetPrimary: (Long) -> Unit,
    onAddRecord: (RecordAction) -> Unit
) {
    val upcoming = remember(vehicle, maintenance, documents) {
        vehicle?.let { DashboardAnalytics.upcoming(it, maintenance, documents) }.orEmpty()
    }
    val costs = remember(maintenance, fuel, expenses) {
        DashboardAnalytics.monthlyCosts(maintenance, fuel, expenses)
    }
    val health = remember(vehicle, maintenance, documents, fuel) {
        vehicle?.let { VehicleHealthEngine.evaluate(it, maintenance, documents, fuel) }
    }
    val fuelInsights = remember(fuel) { FuelAnalytics.analyze(fuel) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RovenaPalette.Accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.45f))
                ) {
                    Text(
                        "R",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = RovenaPalette.Cyan,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Rovena Auto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.p5b_home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(
                        Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp),
                        tint = RovenaPalette.Sky
                    )
                }
            }
        }

        if (vehicles.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vehicles, key = { it.id }) { car ->
                        FilterChip(
                            selected = car.id == vehicle?.id,
                            onClick = { if (car.id != vehicle?.id) onSetPrimary(car.id) },
                            label = { Text(car.nickname.ifBlank { "${car.make} ${car.model}" }) },
                            leadingIcon = { Icon(Icons.Rounded.DirectionsCar, null) }
                        )
                    }
                }
            }
        }

        if (vehicle == null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAddVehicle),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.35f))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.size(64.dp), tint = RovenaPalette.Accent)
                        Text(stringResource(R.string.no_vehicle_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.no_vehicle_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(16.dp), color = RovenaPalette.Accent) {
                            Text(
                                stringResource(R.string.add_vehicle),
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            item {
                VehicleHeroCard(
                    vehicle = vehicle,
                    careScore = health?.score,
                    fuelEfficiency = fuelInsights.averageKmPerLiter,
                    attentionCount = health?.attentionCount ?: 0
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Payments,
                        label = stringResource(R.string.p5b_this_month),
                        value = premiumMoney(costs.current.total, vehicle.currencyCode),
                        accent = RovenaPalette.Cyan
                    )
                    PremiumMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.LocalGasStation,
                        label = stringResource(R.string.p5b_fuel_efficiency),
                        value = fuelInsights.averageKmPerLiter?.let { String.format(Locale.getDefault(), "%.1f km/L", it) } ?: "—",
                        accent = RovenaPalette.Warning
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.p5b_home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PremiumAction(Modifier.weight(1f), Icons.Rounded.Build, stringResource(R.string.maintenance), RovenaPalette.Cyan) {
                        onAddRecord(RecordAction.MAINTENANCE)
                    }
                    PremiumAction(Modifier.weight(1f), Icons.Rounded.LocalGasStation, stringResource(R.string.fuel), RovenaPalette.Warning) {
                        onAddRecord(RecordAction.FUEL)
                    }
                    PremiumAction(Modifier.weight(1f), Icons.Rounded.Payments, stringResource(R.string.expenses), RovenaPalette.Success) {
                        onAddRecord(RecordAction.EXPENSE)
                    }
                    PremiumAction(Modifier.weight(1f), Icons.Rounded.Description, stringResource(R.string.documents), RovenaPalette.Sky) {
                        onAddRecord(RecordAction.DOCUMENT)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.p5b_next_for_car), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.p5b_next_for_car_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = CircleShape, color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                        Icon(Icons.Rounded.Schedule, null, modifier = Modifier.padding(10.dp), tint = RovenaPalette.Accent)
                    }
                }
            }

            if (upcoming.isEmpty()) {
                item {
                    PremiumInfoPanel(
                        title = stringResource(R.string.p5b_nothing_scheduled),
                        body = stringResource(R.string.p5b_nothing_scheduled_body)
                    )
                }
            } else {
                items(upcoming.take(4), key = { "${it.kind}-${it.title}-${it.dueAt}-${it.dueMileage}" }) { item ->
                    PremiumUpcomingCard(item)
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SnapshotMetric(Icons.Rounded.Build, maintenance.size.toString(), stringResource(R.string.maintenance))
                        SnapshotMetric(Icons.Rounded.LocalGasStation, fuel.size.toString(), stringResource(R.string.fuel))
                        SnapshotMetric(Icons.Rounded.Description, documents.size.toString(), stringResource(R.string.documents))
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleHeroCard(
    vehicle: VehicleEntity,
    careScore: Int?,
    fuelEfficiency: Double?,
    attentionCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.55f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0B3555), Color(0xFF071B2D), Color(0xFF0B2842))
                    )
                )
                .padding(20.dp)
        ) {
            Icon(
                Icons.Rounded.DirectionsCar,
                null,
                modifier = Modifier.align(Alignment.Center).size(150.dp).alpha(0.18f),
                tint = RovenaPalette.Cyan
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(18.dp), color = RovenaPalette.Accent.copy(alpha = 0.18f)) {
                        Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.padding(12.dp), tint = RovenaPalette.Cyan)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(vehicle.nickname.ifBlank { "${vehicle.make} ${vehicle.model}" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("${vehicle.make} ${vehicle.model} • ${vehicle.year}", color = RovenaPalette.TextSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (attentionCount == 0) RovenaPalette.Success.copy(alpha = 0.16f) else RovenaPalette.Warning.copy(alpha = 0.16f)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.HealthAndSafety,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (attentionCount == 0) RovenaPalette.Success else RovenaPalette.Warning
                            )
                            Text(
                                careScore?.let { "  $it/100" } ?: "  —",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(56.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroMetric(Icons.Rounded.Speed, NumberFormat.getIntegerInstance().format(vehicle.mileage), stringResource(R.string.odometer))
                    HeroMetric(Icons.Rounded.LocalGasStation, fuelEfficiency?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—", "km/L")
                    HeroMetric(Icons.Rounded.HealthAndSafety, careScore?.toString() ?: "—", stringResource(R.string.p5b_care_score_short))
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = RovenaPalette.Cyan, modifier = Modifier.size(19.dp))
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(label, color = RovenaPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PremiumMetricCard(modifier: Modifier, icon: ImageVector, label: String, value: String, accent: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.13f)) {
                Icon(icon, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = accent)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremiumAction(modifier: Modifier, icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.15f)) {
                Icon(icon, null, modifier = Modifier.padding(9.dp).size(22.dp), tint = accent)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun PremiumUpcomingCard(item: UpcomingItem) {
    val accent = when (item.urgency) {
        UpcomingUrgency.OVERDUE -> RovenaPalette.Danger
        UpcomingUrgency.DUE_SOON -> RovenaPalette.Warning
        UpcomingUrgency.LATER -> RovenaPalette.Cyan
    }
    val icon = if (item.kind == UpcomingKind.MAINTENANCE) Icons.Rounded.Build else Icons.Rounded.Description
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.13f)) {
                    Icon(icon, null, modifier = Modifier.padding(10.dp), tint = accent)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(premiumDueText(item), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.12f)) {
                    Text(premiumStatus(item.urgency), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = accent, fontWeight = FontWeight.Bold)
                }
            }
            val progress = when (item.urgency) {
                UpcomingUrgency.OVERDUE -> 1f
                UpcomingUrgency.DUE_SOON -> 0.78f
                UpcomingUrgency.LATER -> 0.38f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumInfoPanel(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SnapshotMetric(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = RovenaPalette.Cyan, modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

private fun premiumMoney(amount: Double, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(amount)
}.getOrElse { String.format(Locale.getDefault(), "%.2f %s", amount, currencyCode) }

private fun premiumDueText(item: UpcomingItem): String = when {
    item.remainingKm != null -> "${NumberFormat.getIntegerInstance().format(item.remainingKm)} km"
    item.remainingDays != null -> "${item.remainingDays} d"
    item.dueAt != null -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.dueAt))
    else -> "—"
}

private fun premiumStatus(urgency: UpcomingUrgency): String = when (urgency) {
    UpcomingUrgency.OVERDUE -> "!"
    UpcomingUrgency.DUE_SOON -> "Soon"
    UpcomingUrgency.LATER -> "OK"
}
