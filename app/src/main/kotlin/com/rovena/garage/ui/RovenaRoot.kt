package com.rovena.garage.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rovena.garage.R
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.CarRecordRepository
import com.rovena.garage.data.DocumentDraft
import com.rovena.garage.data.ExpenseDraft
import com.rovena.garage.data.FuelDraft
import com.rovena.garage.data.MaintenanceDraft
import com.rovena.garage.data.VehicleReportGenerator
import com.rovena.garage.data.VehicleRepository
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RovenaRoot(
    preferences: AppPreferences,
    vehicleRepository: VehicleRepository,
    recordRepository: CarRecordRepository,
    onLanguageSelected: (String) -> Unit,
    onRequestNotifications: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val onboardingDone by preferences.onboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
    val languageTag by preferences.languageTag.collectAsStateWithLifecycle(initialValue = "")

    if (!onboardingDone) {
        AutomotiveOnboardingScreen(
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
        MainShell(
            preferences = preferences,
            selectedLanguage = languageTag,
            vehicleRepository = vehicleRepository,
            recordRepository = recordRepository,
            onLanguageSelected = onLanguageSelected,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup
        )
    }
}

private enum class MainTab { HOME, CAR, HISTORY, EXPENSES, MORE }

@Composable
private fun MainShell(
    preferences: AppPreferences,
    selectedLanguage: String,
    vehicleRepository: VehicleRepository,
    recordRepository: CarRecordRepository,
    onLanguageSelected: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
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
    val context = LocalContext.current
    val tab = MainTab.entries[tabIndex]

    fun openRecord(action: RecordAction) {
        if (currentVehicle == null) showAddVehicle = true else recordAction = action
    }

    fun shareReport() {
        val vehicle = currentVehicle ?: return
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    VehicleReportGenerator.createPdf(
                        context = context,
                        vehicle = vehicle,
                        maintenance = maintenance,
                        fuel = fuel,
                        expenses = expenses,
                        documents = documents
                    )
                }
            }
            result.onSuccess { file -> VehicleReportGenerator.sharePdf(context, file) }
                .onFailure {
                    Toast.makeText(context, R.string.report_failed, Toast.LENGTH_LONG).show()
                }
        }
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
            MainTab.HOME -> EnhancedDashboardScreen(
                vehicles = vehicles,
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                expenses = expenses,
                documents = documents,
                modifier = Modifier.padding(padding),
                onAddVehicle = { showAddVehicle = true },
                onSetPrimary = { id -> scope.launch { vehicleRepository.setPrimary(id) } },
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
            MainTab.EXPENSES -> EnhancedExpensesScreen(
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                expenses = expenses,
                modifier = Modifier.padding(padding),
                onAddExpense = { openRecord(RecordAction.EXPENSE) }
            )
            MainTab.MORE -> SmartCenterScreen(
                vehicle = currentVehicle,
                maintenance = maintenance,
                fuel = fuel,
                expenses = expenses,
                documents = documents,
                preferences = preferences,
                selectedLanguage = selectedLanguage,
                modifier = Modifier.padding(padding),
                onAddDocument = { openRecord(RecordAction.DOCUMENT) },
                onLanguageSelected = onLanguageSelected,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onShareReport = ::shareReport
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
        Phase5RecordEntrySheet(
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
