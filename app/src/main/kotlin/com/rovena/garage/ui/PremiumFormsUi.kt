package com.rovena.garage.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.DocumentDraft
import com.rovena.garage.data.ExpenseDraft
import com.rovena.garage.data.FuelDraft
import com.rovena.garage.data.MaintenanceDraft
import com.rovena.garage.data.RecordValidator
import com.rovena.garage.ui.theme.RovenaPalette
import com.rovena.garage.ui.theme.RovenaScreenGradient
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun PremiumAutomotiveOnboardingScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onFinished: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        PremiumOnboardingPage(Icons.Rounded.DirectionsCar, R.string.onboarding_title_1, R.string.onboarding_body_1),
        PremiumOnboardingPage(Icons.Rounded.Build, R.string.onboarding_title_2, R.string.onboarding_body_2),
        PremiumOnboardingPage(Icons.Rounded.Payments, R.string.onboarding_title_3, R.string.onboarding_body_3)
    )

    Box(Modifier.fillMaxSize().background(RovenaScreenGradient)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = CircleShape, color = RovenaPalette.Accent.copy(alpha = 0.15f), border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.35f))) {
                        Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.padding(14.dp).size(34.dp), tint = RovenaPalette.Cyan)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.brand_full_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = RovenaPalette.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.onboarding_eyebrow),
                        color = RovenaPalette.Cyan,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = RovenaPalette.Surface,
                    border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.38f))
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(R.drawable.rovena_auto_visual),
                            contentDescription = stringResource(R.string.onboarding_visual_description),
                            modifier = Modifier.fillMaxWidth().height(225.dp).clip(RoundedCornerShape(32.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                            shape = RoundedCornerShape(99.dp),
                            color = RovenaPalette.Navigation,
                            border = BorderStroke(1.dp, RovenaPalette.Outline)
                        ) {
                            Text(
                                stringResource(R.string.onboarding_badge),
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                color = RovenaPalette.Cyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = RovenaPalette.Surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, RovenaPalette.Outline.copy(alpha = 0.85f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        if (page == 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(15.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                                    Icon(Icons.Rounded.Language, null, modifier = Modifier.padding(11.dp), tint = RovenaPalette.Cyan)
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(stringResource(R.string.choose_language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                    Text(stringResource(R.string.onboarding_language_hint), color = RovenaPalette.TextSecondary)
                                }
                            }
                            PremiumLanguageGrid(selectedLanguage, onLanguageSelected)
                        } else {
                            val data = pages[page - 1]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(17.dp), color = RovenaPalette.Accent.copy(alpha = 0.14f)) {
                                    Icon(data.icon, null, modifier = Modifier.padding(13.dp).size(28.dp), tint = RovenaPalette.Cyan)
                                }
                                Text(
                                    stringResource(data.title),
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(stringResource(data.body), style = MaterialTheme.typography.bodyLarge, color = RovenaPalette.TextSecondary)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) { index ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 4.dp).size(if (index == page) 28.dp else 8.dp, 8.dp),
                            shape = RoundedCornerShape(99.dp),
                            color = if (index == page) RovenaPalette.Cyan else RovenaPalette.Outline
                        ) {}
                    }
                }
            }

            item {
                Button(
                    onClick = { if (page < 3) page++ else onFinished() },
                    enabled = page != 0 || selectedLanguage.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (page < 3) stringResource(R.string.continue_label) else stringResource(R.string.get_started), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PremiumRecordEntrySheet(
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
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachmentUri = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RovenaPalette.DeepNavy,
        contentColor = RovenaPalette.TextPrimary,
        dragHandle = {
            Surface(modifier = Modifier.padding(top = 10.dp).size(46.dp, 5.dp), shape = RoundedCornerShape(99.dp), color = RovenaPalette.Outline) {}
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                val icon = premiumRecordIcon(action)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(17.dp), color = RovenaPalette.Accent.copy(alpha = 0.15f)) {
                        Icon(icon, null, modifier = Modifier.padding(12.dp), tint = RovenaPalette.Cyan)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(premiumRecordActionTitle(action), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.record_for_current_vehicle), color = RovenaPalette.TextSecondary)
                    }
                }
            }

            when (action) {
                RecordAction.MAINTENANCE -> {
                    item { PremiumTextInput(primaryText, { primaryText = it; error = null }, stringResource(R.string.service_type)) }
                    item { PremiumTextInput(mileage, { mileage = it.filter(Char::isDigit).take(9); error = null }, stringResource(R.string.odometer), KeyboardType.Number) }
                    item { PremiumTextInput(amount, { amount = premiumSanitizeDecimal(it); error = null }, stringResource(R.string.cost_with_currency, currencyCode), KeyboardType.Decimal) }
                    item { PremiumTextInput(nextDueMileage, { nextDueMileage = it.filter(Char::isDigit).take(9); error = null }, stringResource(R.string.next_due_km_optional), KeyboardType.Number) }
                    item { PremiumTextInput(dateText, { dateText = it.take(10); error = null }, stringResource(R.string.next_due_date_optional), placeholder = "YYYY-MM-DD") }
                }
                RecordAction.FUEL -> {
                    item { PremiumTextInput(mileage, { mileage = it.filter(Char::isDigit).take(9); error = null }, stringResource(R.string.odometer), KeyboardType.Number) }
                    item { PremiumTextInput(liters, { liters = premiumSanitizeDecimal(it); error = null }, stringResource(R.string.liters), KeyboardType.Decimal) }
                    item { PremiumTextInput(amount, { amount = premiumSanitizeDecimal(it); error = null }, stringResource(R.string.total_cost_with_currency, currencyCode), KeyboardType.Decimal) }
                }
                RecordAction.EXPENSE -> {
                    item { PremiumTextInput(primaryText, { primaryText = it; error = null }, stringResource(R.string.expense_category)) }
                    item { PremiumTextInput(amount, { amount = premiumSanitizeDecimal(it); error = null }, stringResource(R.string.amount_with_currency, currencyCode), KeyboardType.Decimal) }
                }
                RecordAction.DOCUMENT -> {
                    item { PremiumTextInput(primaryText, { primaryText = it; error = null }, stringResource(R.string.document_title)) }
                    item { PremiumTextInput(secondaryText, { secondaryText = it; error = null }, stringResource(R.string.document_category)) }
                    item { PremiumTextInput(dateText, { dateText = it.take(10); error = null }, stringResource(R.string.expiry_date_optional), placeholder = "YYYY-MM-DD") }
                }
            }

            item { PremiumTextInput(notes, { notes = it }, stringResource(R.string.notes_optional)) }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { attachmentPicker.launch(arrayOf("application/pdf", "image/*")) },
                    shape = RoundedCornerShape(18.dp),
                    color = RovenaPalette.Accent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, RovenaPalette.Accent.copy(alpha = 0.35f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (attachmentUri == null) Icons.Rounded.AttachFile else Icons.Rounded.CheckCircle, null, tint = if (attachmentUri == null) RovenaPalette.Cyan else RovenaPalette.Success)
                        Text(
                            "  " + stringResource(if (attachmentUri == null) R.string.add_attachment else R.string.attachment_selected),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            error?.let { message ->
                item {
                    Surface(shape = RoundedCornerShape(15.dp), color = RovenaPalette.Danger.copy(alpha = 0.12f), border = BorderStroke(1.dp, RovenaPalette.Danger.copy(alpha = 0.28f))) {
                        Text(message, modifier = Modifier.fillMaxWidth().padding(13.dp), color = RovenaPalette.Danger, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = {
                            val parsedDate = premiumParseOptionalDate(dateText)
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
                                    if (dateValid && RecordValidator.validMaintenance(draft)) { onMaintenance(draft); true } else false
                                }
                                RecordAction.FUEL -> {
                                    val draft = FuelDraft(
                                        mileage = mileage.toLongOrNull() ?: -1L,
                                        liters = liters.toDoubleOrNull() ?: -1.0,
                                        totalCost = amount.toDoubleOrNull() ?: 0.0,
                                        notes = notes,
                                        attachmentUri = attachmentUri
                                    )
                                    if (RecordValidator.validFuel(draft)) { onFuel(draft); true } else false
                                }
                                RecordAction.EXPENSE -> {
                                    val draft = ExpenseDraft(
                                        category = primaryText,
                                        amount = amount.toDoubleOrNull() ?: -1.0,
                                        notes = notes,
                                        attachmentUri = attachmentUri
                                    )
                                    if (RecordValidator.validExpense(draft)) { onExpense(draft); true } else false
                                }
                                RecordAction.DOCUMENT -> {
                                    val draft = DocumentDraft(
                                        title = primaryText,
                                        category = secondaryText,
                                        expiryAt = parsedDate,
                                        notes = notes,
                                        fileUri = attachmentUri
                                    )
                                    if (dateValid && RecordValidator.validDocument(draft)) { onDocument(draft); true } else false
                                }
                            }
                            if (!accepted) error = invalidFields
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(stringResource(R.string.save_record), fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun PremiumLanguageGrid(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    val languages = listOf("ar" to "العربية", "en" to "English", "fr" to "Français", "es" to "Español", "de" to "Deutsch", "tr" to "Türkçe")
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        languages.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { (tag, label) ->
                    val selected = selectedLanguage == tag
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onLanguageSelected(tag) },
                        shape = RoundedCornerShape(17.dp),
                        color = if (selected) RovenaPalette.Accent.copy(alpha = 0.18f) else RovenaPalette.SurfaceRaised,
                        border = BorderStroke(1.dp, if (selected) RovenaPalette.Cyan else RovenaPalette.Outline)
                    ) {
                        Text(
                            if (selected) "✓  $label" else label,
                            modifier = Modifier.padding(vertical = 13.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) RovenaPalette.Cyan else RovenaPalette.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(17.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RovenaPalette.Cyan,
            unfocusedBorderColor = RovenaPalette.Outline,
            focusedLabelColor = RovenaPalette.Cyan,
            cursorColor = RovenaPalette.Cyan,
            focusedContainerColor = RovenaPalette.Surface.copy(alpha = 0.65f),
            unfocusedContainerColor = RovenaPalette.Surface.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun premiumRecordActionTitle(action: RecordAction): String = stringResource(
    when (action) {
        RecordAction.MAINTENANCE -> R.string.add_maintenance
        RecordAction.FUEL -> R.string.add_fuel
        RecordAction.EXPENSE -> R.string.add_expense
        RecordAction.DOCUMENT -> R.string.add_document
    }
)

private fun premiumRecordIcon(action: RecordAction): ImageVector = when (action) {
    RecordAction.MAINTENANCE -> Icons.Rounded.Build
    RecordAction.FUEL -> Icons.Rounded.LocalGasStation
    RecordAction.EXPENSE -> Icons.Rounded.ReceiptLong
    RecordAction.DOCUMENT -> Icons.Rounded.Description
}

private fun premiumSanitizeDecimal(value: String): String {
    val cleaned = value.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot < 0) cleaned.take(12) else {
        val head = cleaned.substring(0, dot + 1)
        val tail = cleaned.substring(dot + 1).replace(".", "")
        (head + tail).take(12)
    }
}

private fun premiumParseOptionalDate(value: String): Long? {
    if (value.isBlank()) return null
    return runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private data class PremiumOnboardingPage(val icon: ImageVector, val title: Int, val body: Int)
