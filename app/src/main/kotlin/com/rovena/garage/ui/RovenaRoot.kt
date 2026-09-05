package com.rovena.garage.ui

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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rovena.garage.R
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.FuelType
import com.rovena.garage.data.VehicleDraft
import com.rovena.garage.data.VehicleRepository
import com.rovena.garage.data.VehicleValidationError
import com.rovena.garage.data.VehicleValidator
import com.rovena.garage.data.local.VehicleEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Year

@Composable
fun RovenaRoot(
    preferences: AppPreferences,
    vehicleRepository: VehicleRepository,
    onLanguageSelected: (String) -> Unit,
    onRequestNotifications: () -> Unit
) {
    val onboardingDone by preferences.onboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
    val languageTag by preferences.languageTag.collectAsStateWithLifecycle(initialValue = "")

    if (!onboardingDone) {
        OnboardingScreen(
            selectedLanguage = languageTag,
            onLanguageSelected = onLanguageSelected,
            onFinished = {
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                scope.launch {
                    preferences.completeOnboarding()
                    onRequestNotifications()
                }
            }
        )
    } else {
        MainShell(vehicleRepository)
    }
}

@Composable
private fun OnboardingScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onFinished: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf(
        R.string.onboarding_title_1,
        R.string.onboarding_title_2,
        R.string.onboarding_title_3
    )
    val bodies = listOf(
        R.string.onboarding_body_1,
        R.string.onboarding_body_2,
        R.string.onboarding_body_3
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("ROVENA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.smart_car_companion), color = MaterialTheme.colorScheme.primary)
        }

        if (page == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.choose_language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                listOf(
                    "ar" to "العربية",
                    "en" to "English",
                    "fr" to "Français",
                    "es" to "Español",
                    "de" to "Deutsch",
                    "tr" to "Türkçe"
                ).forEach { (tag, label) ->
                    Button(
                        onClick = { onLanguageSelected(tag) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedLanguage == tag) "✓  $label" else label)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(titles[page - 1]), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(bodies[page - 1]), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Button(
            onClick = { if (page < 3) page++ else onFinished() },
            enabled = page != 0 || selectedLanguage.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (page < 3) stringResource(R.string.continue_label) else stringResource(R.string.get_started))
        }
    }
}

private enum class MainTab { HOME, CAR, HISTORY, EXPENSES, MORE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(repository: VehicleRepository) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAddVehicle by rememberSaveable { mutableStateOf(false) }
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val vehicles by repository.vehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentVehicle = vehicles.firstOrNull { it.isPrimary } ?: vehicles.firstOrNull()
    val scope = rememberCoroutineScope()
    val tab = MainTab.entries[tabIndex]

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { tabIndex = MainTab.HOME.ordinal },
                    icon = { Icon(Icons.Rounded.Home, null) },
                    label = { Text(stringResource(R.string.home)) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.CAR,
                    onClick = { tabIndex = MainTab.CAR.ordinal },
                    icon = { Icon(Icons.Rounded.DirectionsCar, null) },
                    label = { Text(stringResource(R.string.my_car)) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.HISTORY,
                    onClick = { tabIndex = MainTab.HISTORY.ordinal },
                    icon = { Icon(Icons.Rounded.History, null) },
                    label = { Text(stringResource(R.string.history)) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.EXPENSES,
                    onClick = { tabIndex = MainTab.EXPENSES.ordinal },
                    icon = { Icon(Icons.Rounded.Payments, null) },
                    label = { Text(stringResource(R.string.expenses)) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.MORE,
                    onClick = { tabIndex = MainTab.MORE.ordinal },
                    icon = { Icon(Icons.Rounded.MoreHoriz, null) },
                    label = { Text(stringResource(R.string.more)) }
                )
            }
        }
    ) { padding ->
        when (tab) {
            MainTab.HOME -> DashboardScreen(
                vehicle = currentVehicle,
                modifier = Modifier.padding(padding),
                onAddVehicle = { showAddVehicle = true }
            )
            MainTab.CAR -> GarageScreen(
                vehicles = vehicles,
                modifier = Modifier.padding(padding),
                onAddVehicle = { showAddVehicle = true },
                onSetPrimary = { id -> scope.launch { repository.setPrimary(id) } },
                onDelete = { id -> deleteTargetId = id }
            )
            MainTab.HISTORY -> PlaceholderScreen(
                title = R.string.no_history_title,
                body = R.string.no_history_body,
                modifier = Modifier.padding(padding)
            )
            MainTab.EXPENSES -> PlaceholderScreen(
                title = R.string.no_expenses_title,
                body = R.string.no_expenses_body,
                modifier = Modifier.padding(padding)
            )
            MainTab.MORE -> PlaceholderScreen(
                title = R.string.more_title,
                body = R.string.more_body,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showAddVehicle) {
        AddVehicleSheet(
            onDismiss = { showAddVehicle = false },
            onSave = { draft ->
                scope.launch {
                    repository.addVehicle(draft)
                    showAddVehicle = false
                    tabIndex = MainTab.HOME.ordinal
                }
            }
        )
    }

    vehicles.firstOrNull { it.id == deleteTargetId }?.let { vehicle ->
        DeleteVehicleDialog(
            vehicle = vehicle,
            onDismiss = { deleteTargetId = null },
            onConfirm = {
                val id = vehicle.id
                deleteTargetId = null
                scope.launch { repository.deleteVehicle(id) }
            }
        )
    }
}

@Composable
private fun DashboardScreen(
    vehicle: VehicleEntity?,
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit
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
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.odometer),
                        value = stringResource(R.string.odometer_format, NumberFormat.getIntegerInstance().format(vehicle.mileage))
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.vehicle_year),
                        value = vehicle.year.toString()
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.LocalGasStation,
                        title = stringResource(R.string.fuel_type),
                        value = fuelTypeLabel(vehicle.fuelType)
                    )
                }
            }
            item { HealthWaitingCard() }
        }

        item { Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Modifier.weight(1f), Icons.Rounded.LocalGasStation, R.string.fuel)
                QuickCard(Modifier.weight(1f), Icons.Rounded.Build, R.string.maintenance)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Modifier.weight(1f), Icons.Rounded.ReceiptLong, R.string.expenses)
                QuickCard(Modifier.weight(1f), Icons.Rounded.Description, R.string.documents)
            }
        }
    }
}

@Composable
private fun EmptyVehicleCard(onAddVehicle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.no_vehicle_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.no_vehicle_body))
            Button(onClick = onAddVehicle) {
                Icon(Icons.Rounded.Add, null)
                Text("  ${stringResource(R.string.add_vehicle)}")
            }
        }
    }
}

@Composable
private fun VehicleHeroCard(vehicle: VehicleEntity) {
    val title = vehicleDisplayName(vehicle)
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
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
            if (vehicle.plateNumber.isNotBlank()) {
                Text(vehicle.plateNumber, style = MaterialTheme.typography.bodyLarge)
            }
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
private fun HealthWaitingCard() {
    Card(shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Rounded.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.health_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.health_not_ready_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.health_not_ready_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickCard(modifier: Modifier, icon: ImageVector, title: Int) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GarageScreen(
    vehicles: List<VehicleEntity>,
    modifier: Modifier = Modifier,
    onAddVehicle: () -> Unit,
    onSetPrimary: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.garage_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.garage_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onAddVehicle) { Icon(Icons.Rounded.Add, stringResource(R.string.add_vehicle)) }
            }
        }

        if (vehicles.isEmpty()) {
            item { EmptyVehicleCard(onAddVehicle) }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                GarageVehicleCard(
                    vehicle = vehicle,
                    onSetPrimary = { onSetPrimary(vehicle.id) },
                    onDelete = { onDelete(vehicle.id) }
                )
            }
            item {
                FilledTonalButton(onClick = onAddVehicle, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null)
                    Text("  ${stringResource(R.string.add_vehicle)}")
                }
            }
        }
    }
}

@Composable
private fun GarageVehicleCard(
    vehicle: VehicleEntity,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !vehicle.isPrimary, onClick = onSetPrimary),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vehicle.isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(vehicleDisplayName(vehicle), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${vehicle.make} ${vehicle.model} • ${vehicle.year}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete_vehicle)) }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.odometer_format, NumberFormat.getIntegerInstance().format(vehicle.mileage)))
                if (vehicle.isPrimary) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.primary)
                        Text("  ${stringResource(R.string.current_vehicle)}", fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = onSetPrimary) { Text(stringResource(R.string.set_current)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleSheet(
    onDismiss: () -> Unit,
    onSave: (VehicleDraft) -> Unit
) {
    var make by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf(Year.now().value.toString()) }
    var mileage by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf("") }
    var vin by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("JOD") }
    var fuelName by rememberSaveable { mutableStateOf(FuelType.GASOLINE.name) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val makeError = stringResource(R.string.error_make_required)
    val modelError = stringResource(R.string.error_model_required)
    val yearError = stringResource(R.string.error_year_invalid)
    val mileageError = stringResource(R.string.error_mileage_invalid)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.add_vehicle_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.add_vehicle_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it; validationMessage = null },
                    label = { Text(stringResource(R.string.vehicle_make)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; validationMessage = null },
                    label = { Text(stringResource(R.string.vehicle_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter(Char::isDigit).take(4); validationMessage = null },
                        label = { Text(stringResource(R.string.vehicle_year)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mileage,
                        onValueChange = { mileage = it.filter(Char::isDigit).take(9); validationMessage = null },
                        label = { Text(stringResource(R.string.odometer)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Text(stringResource(R.string.fuel_type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FuelType.entries) { fuel ->
                        FilterChip(
                            selected = fuelName == fuel.name,
                            onClick = { fuelName = fuel.name },
                            label = { Text(fuelTypeLabel(fuel.name)) }
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.nickname_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text(stringResource(R.string.plate_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it.take(17) },
                    label = { Text(stringResource(R.string.vin_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase().filter(Char::isLetter).take(3) },
                    label = { Text(stringResource(R.string.currency)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            validationMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = {
                            val draft = VehicleDraft(
                                make = make,
                                model = model,
                                year = year.toIntOrNull() ?: -1,
                                mileage = mileage.toLongOrNull() ?: -1L,
                                fuelType = runCatching { FuelType.valueOf(fuelName) }.getOrDefault(FuelType.OTHER),
                                nickname = nickname,
                                plateNumber = plate,
                                vin = vin,
                                currencyCode = currency.ifBlank { "JOD" }
                            )
                            validationMessage = when (VehicleValidator.validate(draft)) {
                                VehicleValidationError.MAKE_REQUIRED -> makeError
                                VehicleValidationError.MODEL_REQUIRED -> modelError
                                VehicleValidationError.YEAR_INVALID -> yearError
                                VehicleValidationError.MILEAGE_INVALID -> mileageError
                                null -> null
                            }
                            if (validationMessage == null) onSave(draft)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.save_vehicle)) }
                }
            }
        }
    }
}

@Composable
private fun DeleteVehicleDialog(
    vehicle: VehicleEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_vehicle_title)) },
        text = { Text(stringResource(R.string.delete_vehicle_message, vehicleDisplayName(vehicle))) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun PlaceholderScreen(title: Int, body: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun fuelTypeLabel(value: String): String {
    val fuel = runCatching { FuelType.valueOf(value) }.getOrDefault(FuelType.OTHER)
    return stringResource(
        when (fuel) {
            FuelType.GASOLINE -> R.string.gasoline
            FuelType.DIESEL -> R.string.diesel
            FuelType.HYBRID -> R.string.hybrid
            FuelType.ELECTRIC -> R.string.electric
            FuelType.LPG -> R.string.lpg
            FuelType.OTHER -> R.string.other
        }
    )
}

private fun vehicleDisplayName(vehicle: VehicleEntity): String =
    vehicle.nickname.ifBlank { "${vehicle.make} ${vehicle.model}" }
