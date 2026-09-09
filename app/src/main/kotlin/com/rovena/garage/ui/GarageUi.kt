package com.rovena.garage.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.rovena.garage.ui.theme.RovenaPalette
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
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                    Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.padding(11.dp), tint = RovenaPalette.Cyan)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stringResource(R.string.garage_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.garage_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = RovenaPalette.Accent) {
                    IconButton(onClick = onAddVehicle) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.add_vehicle), tint = Color.White)
                    }
                }
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
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAddVehicle),
                    shape = RoundedCornerShape(20.dp),
                    color = RovenaPalette.Accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.35f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = RovenaPalette.Cyan)
                        Text("  ${stringResource(R.string.add_vehicle)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyVehicleCard(onAddVehicle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
        border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.32f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(shape = CircleShape, color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.padding(18.dp).size(42.dp), tint = RovenaPalette.Cyan)
            }
            Text(stringResource(R.string.no_vehicle_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.no_vehicle_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val accent = if (vehicle.isPrimary) RovenaPalette.Cyan else RovenaPalette.Outline
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !vehicle.isPrimary, onClick = onSetPrimary),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(if (vehicle.isPrimary) 1.4.dp else 1.dp, accent.copy(alpha = if (vehicle.isPrimary) 0.75f else 0.7f))
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0B3555), Color(0xFF071B2D), Color(0xFF0A2740))
                        )
                    )
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    null,
                    modifier = Modifier.align(Alignment.Center).size(110.dp),
                    tint = RovenaPalette.Sky
                )
                if (vehicle.isPrimary) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(99.dp),
                        color = RovenaPalette.Accent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.45f))
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, modifier = Modifier.size(15.dp), tint = RovenaPalette.Cyan)
                            Text("  ${stringResource(R.string.current_vehicle)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(vehicleDisplayName(vehicle), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("${vehicle.make} ${vehicle.model} • ${vehicle.year}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete_vehicle), tint = MaterialTheme.colorScheme.error)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GarageMetric(Icons.Rounded.Speed, NumberFormat.getIntegerInstance().format(vehicle.mileage), stringResource(R.string.odometer))
                    GarageMetric(Icons.Rounded.LocalGasStation, fuelTypeLabel(vehicle.fuelType), stringResource(R.string.fuel_type))
                    GarageMetric(Icons.Rounded.Payments, vehicle.currencyCode, stringResource(R.string.currency))
                }

                if (!vehicle.isPrimary) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onSetPrimary),
                        shape = RoundedCornerShape(16.dp),
                        color = RovenaPalette.Accent.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            stringResource(R.string.set_current),
                            modifier = Modifier.padding(vertical = 12.dp),
                            fontWeight = FontWeight.Bold,
                            color = RovenaPalette.Cyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = RovenaPalette.Cyan)
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
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
