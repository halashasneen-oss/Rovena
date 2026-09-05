package com.rovena.garage.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.DocumentDraft
import com.rovena.garage.data.ExpenseDraft
import com.rovena.garage.data.FuelDraft
import com.rovena.garage.data.MaintenanceDraft
import com.rovena.garage.data.RecordValidator
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class RecordAction { MAINTENANCE, FUEL, EXPENSE, DOCUMENT }

@Composable
internal fun DashboardScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit,
    onAddRecord: (RecordAction) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.dashboard_greeting), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.dashboard_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (vehicle == null) {
            item { EmptyVehicleCard(onAddVehicle) }
        } else {
            item { VehicleHeroCard(vehicle) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        Modifier.weight(1f),
                        Icons.Rounded.Speed,
                        stringResource(R.string.odometer),
                        stringResource(R.string.odometer_format, NumberFormat.getIntegerInstance().format(vehicle.mileage))
                    )
                    MetricCard(
                        Modifier.weight(1f),
                        Icons.Rounded.CalendarMonth,
                        stringResource(R.string.vehicle_year),
                        vehicle.year.toString()
                    )
                    MetricCard(
                        Modifier.weight(1f),
                        Icons.Rounded.LocalGasStation,
                        stringResource(R.string.fuel_type),
                        fuelTypeLabel(vehicle.fuelType)
                    )
                }
            }
            item { VehicleDataHealthCard(vehicle, maintenance, documents) }
            item { MonthlySpendCard(vehicle, maintenance, fuel, expenses) }
        }

        item { Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard(Modifier.weight(1f), Icons.Rounded.LocalGasStation, R.string.fuel) { onAddRecord(RecordAction.FUEL) }
                QuickActionCard(Modifier.weight(1f), Icons.Rounded.Build, R.string.maintenance) { onAddRecord(RecordAction.MAINTENANCE) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard(Modifier.weight(1f), Icons.Rounded.ReceiptLong, R.string.expenses) { onAddRecord(RecordAction.EXPENSE) }
                QuickActionCard(Modifier.weight(1f), Icons.Rounded.Description, R.string.documents) { onAddRecord(RecordAction.DOCUMENT) }
            }
        }
    }
}

@Composable
private fun VehicleHeroCard(vehicle: VehicleEntity) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        Icons.Rounded.DirectionsCar,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(vehicleDisplayName(vehicle), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    if (vehicle.nickname.isNotBlank()) {
                        Text("${vehicle.make} ${vehicle.model}", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    }
                }
                if (vehicle.isPrimary) {
                    Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surface) {
                        Text(
                            stringResource(R.string.primary_badge),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (vehicle.plateNumber.isNotBlank()) Text(vehicle.plateNumber, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier, icon: ImageVector, title: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VehicleDataHealthCard(
    vehicle: VehicleEntity,
    maintenance: List<MaintenanceEntity>,
    documents: List<DocumentEntity>
) {
    val now = System.currentTimeMillis()
    val soon = now + TimeUnit.DAYS.toMillis(30)
    val dueMaintenance = maintenance.count {
        (it.nextDueMileage != null && it.nextDueMileage <= vehicle.mileage) ||
            (it.nextDueAt != null && it.nextDueAt <= now)
    }
    val documentAttention = documents.count { it.expiryAt != null && it.expiryAt <= soon }
    val attention = dueMaintenance + documentAttention

    Card(shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Rounded.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.health_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (maintenance.isEmpty() && documents.isEmpty()) {
                    Text(stringResource(R.string.health_not_ready_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.health_not_ready_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        stringResource(R.string.health_data_summary, maintenance.size, documents.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (attention == 0) stringResource(R.string.nothing_due_now)
                        else stringResource(R.string.items_need_attention, attention),
                        fontWeight = FontWeight.Bold,
                        color = if (attention == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlySpendCard(
    vehicle: VehicleEntity,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>
) {
    val monthStart = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val total = maintenance.filter { it.performedAt >= monthStart }.sumOf { it.cost } +
        fuel.filter { it.filledAt >= monthStart }.sumOf { it.totalCost } +
        expenses.filter { it.spentAt >= monthStart }.sumOf { it.amount }

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Payments, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(stringResource(R.string.costs_this_month), style = MaterialTheme.typography.labelLarge)
                Text(formatMoney(total, vehicle.currencyCode), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun QuickActionCard(modifier: Modifier, icon: ImageVector, title: Int, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.tap_to_add), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class TimelineItem(
    val timestamp: Long,
    val action: RecordAction,
    val title: String,
    val detail: String,
    val amount: Double = 0.0
)

@Composable
internal fun HistoryScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier,
    onAddRecord: (RecordAction) -> Unit
) {
    val timeline = buildTimeline(maintenance, fuel, expenses, documents)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.history_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SmallAddButton(Icons.Rounded.Build, R.string.maintenance) { onAddRecord(RecordAction.MAINTENANCE) } }
                item { SmallAddButton(Icons.Rounded.LocalGasStation, R.string.fuel) { onAddRecord(RecordAction.FUEL) } }
                item { SmallAddButton(Icons.Rounded.ReceiptLong, R.string.expenses) { onAddRecord(RecordAction.EXPENSE) } }
                item { SmallAddButton(Icons.Rounded.Description, R.string.documents) { onAddRecord(RecordAction.DOCUMENT) } }
            }
        }
        if (vehicle == null) {
            item { EmptyStateCard(R.string.no_vehicle_title, R.string.no_vehicle_body) }
        } else if (timeline.isEmpty()) {
            item { EmptyStateCard(R.string.no_history_title, R.string.no_history_body) }
        } else {
            items(timeline) { item -> TimelineCard(item, vehicle.currencyCode) }
        }
    }
}

@Composable
private fun SmallAddButton(icon: ImageVector, title: Int, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(icon, null)
        Text("  ${stringResource(title)}")
    }
}

@Composable
private fun TimelineCard(item: TimelineItem, currencyCode: String) {
    val pair = when (item.action) {
        RecordAction.MAINTENANCE -> Icons.Rounded.Build to R.string.maintenance
        RecordAction.FUEL -> Icons.Rounded.LocalGasStation to R.string.fuel
        RecordAction.EXPENSE -> Icons.Rounded.ReceiptLong to R.string.expenses
        RecordAction.DOCUMENT -> Icons.Rounded.Description to R.string.documents
    }
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(pair.first, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(pair.second), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(item.title, fontWeight = FontWeight.Bold)
                Text("${formatDate(item.timestamp)} • ${item.detail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.amount > 0.0) Text(formatMoney(item.amount, currencyCode), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ExpensesScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    modifier: Modifier = Modifier,
    onAddExpense: () -> Unit
) {
    val monthStart = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthly = maintenance.filter { it.performedAt >= monthStart }.sumOf { it.cost } +
        fuel.filter { it.filledAt >= monthStart }.sumOf { it.totalCost } +
        expenses.filter { it.spentAt >= monthStart }.sumOf { it.amount }
    val allTime = maintenance.sumOf { it.cost } + fuel.sumOf { it.totalCost } + expenses.sumOf { it.amount }
    val costItems = buildTimeline(maintenance, fuel, expenses, emptyList()).filter { it.amount > 0.0 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.expenses), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.expenses_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onAddExpense) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  ${stringResource(R.string.add_expense)}")
                }
            }
        }
        if (vehicle == null) {
            item { EmptyStateCard(R.string.no_vehicle_title, R.string.no_vehicle_body) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoneyMetric(Modifier.weight(1f), R.string.monthly_spend, monthly, vehicle.currencyCode)
                    MoneyMetric(Modifier.weight(1f), R.string.all_time_spend, allTime, vehicle.currencyCode)
                }
            }
            if (costItems.isEmpty()) {
                item { EmptyStateCard(R.string.no_expenses_title, R.string.no_expenses_body) }
            } else {
                item { Text(stringResource(R.string.recent_costs), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(costItems.take(30)) { item -> TimelineCard(item, vehicle.currencyCode) }
            }
        }
    }
}

@Composable
private fun MoneyMetric(modifier: Modifier, title: Int, amount: Double, currencyCode: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(amount, currencyCode), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun MoreScreen(
    vehicle: VehicleEntity?,
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier,
    onAddDocument: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.documents), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.documents_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onAddDocument) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  ${stringResource(R.string.add_document)}")
                }
            }
        }
        if (vehicle == null) {
            item { EmptyStateCard(R.string.no_vehicle_title, R.string.no_vehicle_body) }
        } else if (documents.isEmpty()) {
            item { EmptyStateCard(R.string.no_documents_title, R.string.no_documents_body) }
        } else {
            items(documents, key = { it.id }) { document -> DocumentCard(document) }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.more_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.more_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(document: DocumentEntity) {
    val expiryText = if (document.expiryAt != null) {
        stringResource(R.string.expires_on, formatDate(document.expiryAt))
    } else {
        stringResource(R.string.no_expiry)
    }
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(document.title, fontWeight = FontWeight.Bold)
                Text(document.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(expiryText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: Int, body: Int) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordEntrySheet(
    action: RecordAction,
    currentMileage: Long,
    currencyCode: String,
    onDismiss: () -> Unit,
    onMaintenance: (MaintenanceDraft) -> Unit,
    onFuel: (FuelDraft) -> Unit,
    onExpense: (ExpenseDraft) -> Unit,
    onDocument: (DocumentDraft) -> Unit
) {
    var primaryText by rememberSaveable(action) { mutableStateOf("") }
    var secondaryText by rememberSaveable(action) { mutableStateOf("") }
    var mileage by rememberSaveable(action) { mutableStateOf(currentMileage.toString()) }
    var amount by rememberSaveable(action) { mutableStateOf("") }
    var liters by rememberSaveable(action) { mutableStateOf("") }
    var notes by rememberSaveable(action) { mutableStateOf("") }
    var nextDueMileage by rememberSaveable(action) { mutableStateOf("") }
    var dateText by rememberSaveable(action) { mutableStateOf("") }
    var error by rememberSaveable(action) { mutableStateOf<String?>(null) }
    val invalidFields = stringResource(R.string.error_record_fields)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(recordActionTitle(action), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.record_for_current_vehicle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when (action) {
                RecordAction.MAINTENANCE -> {
                    item { TextInput(primaryText, { primaryText = it; error = null }, R.string.service_type) }
                    item { NumberInput(mileage, { mileage = it.filter(Char::isDigit).take(9); error = null }, R.string.odometer) }
                    item { DecimalInput(amount, { amount = sanitizeDecimal(it); error = null }, R.string.cost_with_currency, currencyCode) }
                    item { NumberInput(nextDueMileage, { nextDueMileage = it.filter(Char::isDigit).take(9); error = null }, R.string.next_due_km_optional) }
                    item { TextInput(dateText, { dateText = it.take(10); error = null }, R.string.next_due_date_optional, "YYYY-MM-DD") }
                }
                RecordAction.FUEL -> {
                    item { NumberInput(mileage, { mileage = it.filter(Char::isDigit).take(9); error = null }, R.string.odometer) }
                    item { DecimalInput(liters, { liters = sanitizeDecimal(it); error = null }, R.string.liters) }
                    item { DecimalInput(amount, { amount = sanitizeDecimal(it); error = null }, R.string.total_cost_with_currency, currencyCode) }
                }
                RecordAction.EXPENSE -> {
                    item { TextInput(primaryText, { primaryText = it; error = null }, R.string.expense_category) }
                    item { DecimalInput(amount, { amount = sanitizeDecimal(it); error = null }, R.string.amount_with_currency, currencyCode) }
                }
                RecordAction.DOCUMENT -> {
                    item { TextInput(primaryText, { primaryText = it; error = null }, R.string.document_title) }
                    item { TextInput(secondaryText, { secondaryText = it; error = null }, R.string.document_category) }
                    item { TextInput(dateText, { dateText = it.take(10); error = null }, R.string.expiry_date_optional, "YYYY-MM-DD") }
                }
            }

            item { TextInput(notes, { notes = it }, R.string.notes_optional) }

            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = {
                            val parsedDate = parseOptionalDate(dateText)
                            val dateValid = dateText.isBlank() || parsedDate != null
                            val accepted = when (action) {
                                RecordAction.MAINTENANCE -> {
                                    val draft = MaintenanceDraft(
                                        serviceType = primaryText,
                                        mileage = mileage.toLongOrNull() ?: -1L,
                                        cost = amount.toDoubleOrNull() ?: 0.0,
                                        notes = notes,
                                        nextDueMileage = nextDueMileage.toLongOrNull(),
                                        nextDueAt = parsedDate
                                    )
                                    if (dateValid && RecordValidator.validMaintenance(draft)) {
                                        onMaintenance(draft)
                                        true
                                    } else false
                                }
                                RecordAction.FUEL -> {
                                    val draft = FuelDraft(
                                        mileage = mileage.toLongOrNull() ?: -1L,
                                        liters = liters.toDoubleOrNull() ?: -1.0,
                                        totalCost = amount.toDoubleOrNull() ?: 0.0,
                                        notes = notes
                                    )
                                    if (RecordValidator.validFuel(draft)) {
                                        onFuel(draft)
                                        true
                                    } else false
                                }
                                RecordAction.EXPENSE -> {
                                    val draft = ExpenseDraft(primaryText, amount.toDoubleOrNull() ?: -1.0, notes)
                                    if (RecordValidator.validExpense(draft)) {
                                        onExpense(draft)
                                        true
                                    } else false
                                }
                                RecordAction.DOCUMENT -> {
                                    val draft = DocumentDraft(
                                        title = primaryText,
                                        category = secondaryText,
                                        expiryAt = parsedDate,
                                        notes = notes
                                    )
                                    if (dateValid && RecordValidator.validDocument(draft)) {
                                        onDocument(draft)
                                        true
                                    } else false
                                }
                            }
                            if (!accepted) error = invalidFields
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save_record))
                    }
                }
            }
        }
    }
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, label: Int, placeholderText: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        placeholder = if (placeholderText != null) {
            { Text(placeholderText) }
        } else {
            null
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NumberInput(value: String, onValueChange: (String) -> Unit, label: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DecimalInput(value: String, onValueChange: (String) -> Unit, label: Int, currencyCode: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                if (currencyCode == null) stringResource(label)
                else stringResource(label, currencyCode)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun recordActionTitle(action: RecordAction): String = stringResource(
    when (action) {
        RecordAction.MAINTENANCE -> R.string.add_maintenance
        RecordAction.FUEL -> R.string.add_fuel
        RecordAction.EXPENSE -> R.string.add_expense
        RecordAction.DOCUMENT -> R.string.add_document
    }
)

private fun buildTimeline(
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
    expenses: List<ExpenseEntity>,
    documents: List<DocumentEntity>
): List<TimelineItem> {
    val result = mutableListOf<TimelineItem>()
    maintenance.forEach {
        result += TimelineItem(it.performedAt, RecordAction.MAINTENANCE, it.serviceType, "${NumberFormat.getIntegerInstance().format(it.mileage)} km", it.cost)
    }
    fuel.forEach {
        result += TimelineItem(it.filledAt, RecordAction.FUEL, "${formatDecimal(it.liters)} L", "${NumberFormat.getIntegerInstance().format(it.mileage)} km", it.totalCost)
    }
    expenses.forEach {
        result += TimelineItem(it.spentAt, RecordAction.EXPENSE, it.category, it.notes.ifBlank { "—" }, it.amount)
    }
    documents.forEach {
        result += TimelineItem(it.createdAt, RecordAction.DOCUMENT, it.title, it.category)
    }
    return result.sortedByDescending { it.timestamp }
}

private fun sanitizeDecimal(value: String): String {
    val cleaned = value.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot < 0) cleaned.take(12) else {
        val head = cleaned.substring(0, dot + 1)
        val tail = cleaned.substring(dot + 1).replace(".", "")
        (head + tail).take(12)
    }
}

private fun parseOptionalDate(value: String): Long? {
    if (value.isBlank()) return null
    return runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun formatDate(epochMillis: Long): String = runCatching {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
}.getOrDefault("")

private fun formatMoney(amount: Double, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(amount)
}.getOrElse { "${formatDecimal(amount)} $currencyCode" }

private fun formatDecimal(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
}.format(value)
