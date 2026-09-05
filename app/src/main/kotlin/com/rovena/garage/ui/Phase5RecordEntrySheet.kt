package com.rovena.garage.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Phase5RecordEntrySheet(
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
    var attachmentUri by rememberSaveable(action) { mutableStateOf<String?>(null) }
    var error by rememberSaveable(action) { mutableStateOf<String?>(null) }
    val invalidFields = stringResource(R.string.error_record_fields)
    val context = LocalContext.current
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            attachmentUri = uri.toString()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(phase5RecordActionTitle(action), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.record_for_current_vehicle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when (action) {
                RecordAction.MAINTENANCE -> {
                    item { Phase5TextInput(primaryText, { primaryText = it; error = null }, R.string.service_type) }
                    item { Phase5NumberInput(mileage, { mileage = it.filter(Char::isDigit).take(9); error = null }, R.string.odometer) }
                    item { Phase5DecimalInput(amount, { amount = phase5SanitizeDecimal(it); error = null }, R.string.cost_with_currency, currencyCode) }
                    item { Phase5NumberInput(nextDueMileage, { nextDueMileage = it.filter(Char::isDigit).take(9); error = null }, R.string.next_due_km_optional) }
                    item { Phase5TextInput(dateText, { dateText = it.take(10); error = null }, R.string.next_due_date_optional, "YYYY-MM-DD") }
                }
                RecordAction.FUEL -> {
                    item { Phase5NumberInput(mileage, { mileage = it.filter(Char::isDigit).take(9); error = null }, R.string.odometer) }
                    item { Phase5DecimalInput(liters, { liters = phase5SanitizeDecimal(it); error = null }, R.string.liters) }
                    item { Phase5DecimalInput(amount, { amount = phase5SanitizeDecimal(it); error = null }, R.string.total_cost_with_currency, currencyCode) }
                }
                RecordAction.EXPENSE -> {
                    item { Phase5TextInput(primaryText, { primaryText = it; error = null }, R.string.expense_category) }
                    item { Phase5DecimalInput(amount, { amount = phase5SanitizeDecimal(it); error = null }, R.string.amount_with_currency, currencyCode) }
                }
                RecordAction.DOCUMENT -> {
                    item { Phase5TextInput(primaryText, { primaryText = it; error = null }, R.string.document_title) }
                    item { Phase5TextInput(secondaryText, { secondaryText = it; error = null }, R.string.document_category) }
                    item { Phase5TextInput(dateText, { dateText = it.take(10); error = null }, R.string.expiry_date_optional, "YYYY-MM-DD") }
                }
            }

            item { Phase5TextInput(notes, { notes = it }, R.string.notes_optional) }
            item {
                FilledTonalButton(
                    onClick = { attachmentPicker.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (attachmentUri == null) Icons.Rounded.AttachFile else Icons.Rounded.CheckCircle, null)
                    Text(
                        "  " + stringResource(
                            if (attachmentUri == null) R.string.add_attachment else R.string.attachment_selected
                        )
                    )
                }
            }

            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = {
                            val parsedDate = phase5ParseOptionalDate(dateText)
                            val dateValid = dateText.isBlank() || parsedDate != null
                            val accepted = when (action) {
                                RecordAction.MAINTENANCE -> {
                                    val draft = MaintenanceDraft(
                                        serviceType = primaryText,
                                        mileage = mileage.toLongOrNull() ?: -1L,
                                        cost = amount.toDoubleOrNull() ?: 0.0,
                                        notes = notes,
                                        nextDueMileage = nextDueMileage.toLongOrNull(),
                                        nextDueAt = parsedDate,
                                        attachmentUri = attachmentUri
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
                                        notes = notes,
                                        attachmentUri = attachmentUri
                                    )
                                    if (RecordValidator.validFuel(draft)) {
                                        onFuel(draft)
                                        true
                                    } else false
                                }
                                RecordAction.EXPENSE -> {
                                    val draft = ExpenseDraft(
                                        category = primaryText,
                                        amount = amount.toDoubleOrNull() ?: -1.0,
                                        notes = notes,
                                        attachmentUri = attachmentUri
                                    )
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
                                        notes = notes,
                                        fileUri = attachmentUri
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
private fun Phase5TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    placeholderText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        placeholder = placeholderText?.let { text -> { Text(text) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Phase5NumberInput(value: String, onValueChange: (String) -> Unit, label: Int) {
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
private fun Phase5DecimalInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    currencyCode: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(if (currencyCode == null) stringResource(label) else stringResource(label, currencyCode))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun phase5RecordActionTitle(action: RecordAction): String = stringResource(
    when (action) {
        RecordAction.MAINTENANCE -> R.string.add_maintenance
        RecordAction.FUEL -> R.string.add_fuel
        RecordAction.EXPENSE -> R.string.add_expense
        RecordAction.DOCUMENT -> R.string.add_document
    }
)

private fun phase5SanitizeDecimal(value: String): String {
    val cleaned = value.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot < 0) cleaned.take(12) else {
        val head = cleaned.substring(0, dot + 1)
        val tail = cleaned.substring(dot + 1).replace(".", "")
        (head + tail).take(12)
    }
}

private fun phase5ParseOptionalDate(value: String): Long? {
    if (value.isBlank()) return null
    return runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}
