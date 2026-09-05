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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.FuelType
import com.rovena.garage.data.VehicleDraft
import com.rovena.garage.data.VehicleValidationError
import com.rovena.garage.data.VehicleValidator
import com.rovena.garage.data.local.VehicleEntity
import java.text.NumberFormat
import java.time.Year

@Composable
internal fun GarageScreen(
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
internal fun EmptyVehicleCard(onAddVehicle: () -> Unit) {
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
internal fun AddVehicleSheet(
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
internal fun DeleteVehicleDialog(
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
internal fun fuelTypeLabel(value: String): String {
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

internal fun vehicleDisplayName(vehicle: VehicleEntity): String =
    vehicle.nickname.ifBlank { "${vehicle.make} ${vehicle.model}" }
