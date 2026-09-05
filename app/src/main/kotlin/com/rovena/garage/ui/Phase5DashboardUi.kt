package com.rovena.garage.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun EnhancedDashboardScreen(
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.p5b_home_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.p5b_home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (vehicles.size > 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.p5b_switch_car), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vehicles, key = { it.id }) { car ->
                            FilterChip(
                                selected = car.id == vehicle?.id,
                                onClick = { if (car.id != vehicle?.id) onSetPrimary(car.id) },
                                label = { Text(carLabel(car)) },
                                leadingIcon = { Icon(Icons.Rounded.DirectionsCar, null) }
                            )
                        }
                    }
                }
            }
        }

        if (vehicle == null) {
            item {
                Card(shape = RoundedCornerShape(26.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.no_vehicle_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.no_vehicle_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = onAddVehicle) { Text(stringResource(R.string.add_vehicle)) }
                    }
                }
            }
        } else {
            item { PremiumVehicleHero(vehicle, health?.score, upcoming.count { it.urgency != UpcomingUrgency.LATER }) }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Payments,
                        label = stringResource(R.string.p5b_this_month),
                        value = money(costs.current.total, vehicle.currencyCode)
                    )
                    DashboardMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.LocalGasStation,
                        label = stringResource(R.string.p5b_fuel_efficiency),
                        value = fuelInsights.averageKmPerLiter?.let { "${number(it, 1)} km/L" } ?: "—"
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Speed,
                        label = stringResource(R.string.p5b_cost_100km),
                        value = costs.costPer100Km?.let { money(it, vehicle.currencyCode) } ?: "—"
                    )
                    DashboardMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Schedule,
                        label = stringResource(R.string.p5b_vs_last_month),
                        value = costChangeText(costs.changePercent)
                    )
                }
            }

            item {
                Text(stringResource(R.string.p5b_next_for_car), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.p5b_next_for_car_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (upcoming.isEmpty()) {
                item { InfoCard(stringResource(R.string.p5b_nothing_scheduled), stringResource(R.string.p5b_nothing_scheduled_body)) }
            } else {
                items(upcoming.take(5), key = { "${it.kind}-${it.title}-${it.dueAt}-${it.dueMileage}" }) { item ->
                    UpcomingCard(item)
                }
            }

            item {
                Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(Modifier.weight(1f), Icons.Rounded.LocalGasStation, stringResource(R.string.fuel)) {
                        onAddRecord(RecordAction.FUEL)
                    }
                    ActionCard(Modifier.weight(1f), Icons.Rounded.Build, stringResource(R.string.maintenance)) {
                        onAddRecord(RecordAction.MAINTENANCE)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(Modifier.weight(1f), Icons.Rounded.ReceiptLong, stringResource(R.string.expenses)) {
                        onAddRecord(RecordAction.EXPENSE)
                    }
                    ActionCard(Modifier.weight(1f), Icons.Rounded.Description, stringResource(R.string.documents)) {
                        onAddRecord(RecordAction.DOCUMENT)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EnhancedExpensesScreen(
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.p5b_costs_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.p5b_costs_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onAddExpense) { Text(stringResource(R.string.add_expense)) }
            }
        }

        if (vehicle == null) {
            item { InfoCard(stringResource(R.string.no_vehicle_title), stringResource(R.string.no_vehicle_body)) }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.p5b_this_month), style = MaterialTheme.typography.labelLarge)
                        Text(money(analytics.current.total, vehicle.currencyCode), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(
                            stringResource(R.string.p5b_previous_month_value, money(analytics.previous.total, vehicle.currencyCode)),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(costChangeSentence(analytics.changePercent), fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CostTypeCard(Modifier.weight(1f), Icons.Rounded.LocalGasStation, R.string.fuel, analytics.current.fuel, vehicle.currencyCode)
                    CostTypeCard(Modifier.weight(1f), Icons.Rounded.Build, R.string.maintenance, analytics.current.maintenance, vehicle.currencyCode)
                    CostTypeCard(Modifier.weight(1f), Icons.Rounded.ReceiptLong, R.string.p5b_other, analytics.current.other, vehicle.currencyCode)
                }
            }

            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.p5b_cost_100km), fontWeight = FontWeight.Bold)
                            Text(
                                analytics.distanceKm?.let { stringResource(R.string.p5b_distance_basis, NumberFormat.getIntegerInstance().format(it)) }
                                    ?: stringResource(R.string.p5b_distance_not_enough),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            analytics.costPer100Km?.let { money(it, vehicle.currencyCode) } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            item { Text(stringResource(R.string.p5b_top_categories), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (analytics.topOtherCategories.isEmpty()) {
                item { InfoCard(stringResource(R.string.p5b_no_categories), stringResource(R.string.p5b_no_categories_body)) }
            } else {
                items(analytics.topOtherCategories, key = { it.first }) { (category, amount) ->
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Payments, null, tint = MaterialTheme.colorScheme.primary)
                            Text(category, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
                            Text(money(amount, vehicle.currencyCode), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumVehicleHero(vehicle: VehicleEntity, score: Int?, attentionCount: Int) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(14.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(carLabel(vehicle), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("${vehicle.year} • ${NumberFormat.getIntegerInstance().format(vehicle.mileage)} km", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(score?.let { "$it/100" } ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.p5b_care_score_short), style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HealthAndSafety, null, tint = if (attentionCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Text(
                        if (attentionCount > 0) stringResource(R.string.p5b_attention_items, attentionCount)
                        else stringResource(R.string.p5b_no_attention),
                        modifier = Modifier.padding(horizontal = 10.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun UpcomingCard(item: UpcomingItem) {
    val icon = if (item.kind == UpcomingKind.MAINTENANCE) Icons.Rounded.Build else Icons.Rounded.Description
    val tint = when (item.urgency) {
        UpcomingUrgency.OVERDUE -> MaterialTheme.colorScheme.error
        UpcomingUrgency.DUE_SOON -> MaterialTheme.colorScheme.tertiary
        UpcomingUrgency.LATER -> MaterialTheme.colorScheme.primary
    }
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(upcomingDetail(item), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = tint.copy(alpha = 0.14f)) {
                Text(upcomingStatus(item.urgency), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = tint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun upcomingDetail(item: UpcomingItem): String {
    val km = item.remainingKm
    val days = item.remainingDays
    return when {
        item.urgency == UpcomingUrgency.OVERDUE && km != null && km <= 0L -> stringResource(R.string.p5b_overdue_by_km, NumberFormat.getIntegerInstance().format(-km))
        item.urgency == UpcomingUrgency.OVERDUE && days != null && days <= 0L -> stringResource(R.string.p5b_overdue_by_days, -days)
        km != null && days != null -> stringResource(R.string.p5b_due_km_days, NumberFormat.getIntegerInstance().format(km.coerceAtLeast(0L)), days.coerceAtLeast(0L))
        km != null -> stringResource(R.string.p5b_due_in_km, NumberFormat.getIntegerInstance().format(km.coerceAtLeast(0L)))
        days != null -> stringResource(R.string.p5b_due_in_days, days.coerceAtLeast(0L))
        else -> stringResource(R.string.p5b_scheduled)
    }
}

@Composable
private fun upcomingStatus(urgency: UpcomingUrgency): String = stringResource(
    when (urgency) {
        UpcomingUrgency.OVERDUE -> R.string.p5b_overdue
        UpcomingUrgency.DUE_SOON -> R.string.p5b_due_soon
        UpcomingUrgency.LATER -> R.string.p5b_later
    }
)

@Composable
private fun ActionCard(modifier: Modifier, icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tap_to_add), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CostTypeCard(modifier: Modifier, icon: ImageVector, label: Int, amount: Double, currencyCode: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
            Text(money(amount, currencyCode), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun costChangeText(percent: Double?): String = when {
    percent == null -> stringResource(R.string.p5b_new_spend)
    percent > 0.5 -> "+${number(percent, 0)}%"
    percent < -0.5 -> "${number(percent, 0)}%"
    else -> stringResource(R.string.p5b_stable)
}

@Composable
private fun costChangeSentence(percent: Double?): String = when {
    percent == null -> stringResource(R.string.p5b_no_previous_baseline)
    percent > 0.5 -> stringResource(R.string.p5b_spend_up, number(percent, 0))
    percent < -0.5 -> stringResource(R.string.p5b_spend_down, number(-percent, 0))
    else -> stringResource(R.string.p5b_spend_stable)
}

private fun carLabel(vehicle: VehicleEntity): String = vehicle.nickname.takeIf { it.isNotBlank() }
    ?: "${vehicle.make} ${vehicle.model}"

private fun money(value: Double, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(value)
}.getOrElse { "${number(value, 2)} $currencyCode" }

private fun number(value: Double, digits: Int): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = digits
    minimumFractionDigits = 0
}.format(value)
