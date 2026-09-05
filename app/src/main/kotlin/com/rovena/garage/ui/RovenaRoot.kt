package com.rovena.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rovena.garage.R
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.CarRecordRepository
import com.rovena.garage.data.DocumentDraft
import com.rovena.garage.data.ExpenseDraft
import com.rovena.garage.data.FuelDraft
import com.rovena.garage.data.MaintenanceDraft
import com.rovena.garage.data.VehicleRepository
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun RovenaRoot(
    preferences: AppPreferences,
    vehicleRepository: VehicleRepository,
    recordRepository: CarRecordRepository,
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
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    preferences.completeOnboarding()
                    onRequestNotifications()
                }
            }
        )
    } else {
        MainShell(vehicleRepository, recordRepository)
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
                    Button(onClick = { onLanguageSelected(tag) }, modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun MainShell(
    vehicleRepository: VehicleRepository,
    recordRepository: CarRecordRepository
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAddVehicle by rememberSaveable { mutableStateOf(false) }
    var deleteTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordAction by rememberSaveable { mutableStateOf<RecordAction?>(null) }

    val vehicles by vehicleRepository.vehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentVehicle = vehicles.firstOrNull { it.isPrimary } ?: vehicles.firstOrNull()
    val currentId = currentVehicle?.id

    val maintenanceFlow = remember(currentId) {
        currentId?.let(recordRepository::maintenance) ?: flowOf(emptyList<MaintenanceEntity>())
    }
    val fuelFlow = remember(currentId) {
        currentId?.let(recordRepository::fuel) ?: flowOf(emptyList<FuelEntryEntity>())
    }
    val expenseFlow = remember(currentId) {
        currentId?.let(recordRepository::expenses) ?: flowOf(emptyList<ExpenseEntity>())
    }
    val documentFlow = remember(currentId) {
        currentId?.let(recordRepository::documents) ?: flowOf(emptyList<DocumentEntity>())
    }

    val maintenance by maintenanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val fuel by fuelFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by expenseFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val documents by documentFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val tab = MainTab.entries[tabIndex]

    fun openRecord(action: RecordAction) {
        if (currentVehicle == null) showAddVehicle = true else recordAction = action
    }

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
                    label = { Text(stringResource(R.string.insights)) }
                )
            }
        }
    ) { padding ->
        when (tab) {
            MainTab.HOME -> DashboardScreen(
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                expenses = expenses,
                documents = documents,
                modifier = Modifier.padding(padding),
                onAddVehicle = { showAddVehicle = true },
                onAddRecord = ::openRecord
            )
            MainTab.CAR -> GarageScreen(
                vehicles = vehicles,
                modifier = Modifier.padding(padding),
                onAddVehicle = { showAddVehicle = true },
                onSetPrimary = { id -> scope.launch { vehicleRepository.setPrimary(id) } },
                onDelete = { id -> deleteTargetId = id }
            )
            MainTab.HISTORY -> HistoryScreen(
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                expenses = expenses,
                documents = documents,
                modifier = Modifier.padding(padding),
                onAddRecord = ::openRecord
            )
            MainTab.EXPENSES -> ExpensesScreen(
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                expenses = expenses,
                modifier = Modifier.padding(padding),
                onAddExpense = { openRecord(RecordAction.EXPENSE) }
            )
            MainTab.MORE -> SmartHubScreen(
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                documents = documents,
                modifier = Modifier.padding(padding),
                onAddDocument = { openRecord(RecordAction.DOCUMENT) }
            )
        }
    }

    if (showAddVehicle) {
        AddVehicleSheet(
            onDismiss = { showAddVehicle = false },
            onSave = { draft ->
                scope.launch {
                    vehicleRepository.addVehicle(draft)
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
                scope.launch { vehicleRepository.deleteVehicle(id) }
            }
        )
    }

    val activeVehicleId = currentVehicle?.id
    if (recordAction != null && activeVehicleId != null) {
        RecordEntrySheet(
            action = recordAction!!,
            currentMileage = currentVehicle.mileage,
            currencyCode = currentVehicle.currencyCode,
            onDismiss = { recordAction = null },
            onMaintenance = { draft: MaintenanceDraft ->
                scope.launch {
                    recordRepository.addMaintenance(activeVehicleId, draft)
                    recordAction = null
                }
            },
            onFuel = { draft: FuelDraft ->
                scope.launch {
                    recordRepository.addFuel(activeVehicleId, draft)
                    recordAction = null
                }
            },
            onExpense = { draft: ExpenseDraft ->
                scope.launch {
                    recordRepository.addExpense(activeVehicleId, draft)
                    recordAction = null
                }
            },
            onDocument = { draft: DocumentDraft ->
                scope.launch {
                    recordRepository.addDocument(activeVehicleId, draft)
                    recordAction = null
                }
            }
        )
    }
}
