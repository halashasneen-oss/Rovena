package com.rovena.garage.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rovena.garage.R

@Composable
internal fun AutomotiveOnboardingScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onFinished: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(Icons.Rounded.DirectionsCar, R.string.onboarding_title_1, R.string.onboarding_body_1),
        OnboardingPage(Icons.Rounded.Build, R.string.onboarding_title_2, R.string.onboarding_body_2),
        OnboardingPage(Icons.Rounded.Payments, R.string.onboarding_title_3, R.string.onboarding_body_3)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.brand_full_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_eyebrow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.rovena_app_visual),
                        contentDescription = stringResource(R.string.onboarding_visual_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(Icons.Rounded.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.onboarding_badge),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (page == 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(
                                    Icons.Rounded.Language,
                                    null,
                                    modifier = Modifier.padding(10.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.choose_language),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.onboarding_language_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LanguageGrid(selectedLanguage, onLanguageSelected)
                    } else {
                        val data = pages[page - 1]
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(
                                    data.icon,
                                    null,
                                    modifier = Modifier.padding(13.dp).size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                stringResource(data.title),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            stringResource(data.body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    Surface(
                        modifier = Modifier.padding(horizontal = 4.dp).size(if (index == page) 24.dp else 8.dp, 8.dp),
                        shape = RoundedCornerShape(99.dp),
                        color = if (index == page) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }
            }
        }

        item {
            Button(
                onClick = { if (page < 3) page++ else onFinished() },
                enabled = page != 0 || selectedLanguage.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    if (page < 3) stringResource(R.string.continue_label) else stringResource(R.string.get_started),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LanguageGrid(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    val languages = listOf(
        "ar" to "العربية",
        "en" to "English",
        "fr" to "Français",
        "es" to "Español",
        "de" to "Deutsch",
        "tr" to "Türkçe"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        languages.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (tag, label) ->
                    if (selectedLanguage == tag) {
                        FilledTonalButton(
                            onClick = { onLanguageSelected(tag) },
                            modifier = Modifier.weight(1f)
                        ) { Text("✓  $label", fontWeight = FontWeight.Bold) }
                    } else {
                        OutlinedButton(
                            onClick = { onLanguageSelected(tag) },
                            modifier = Modifier.weight(1f)
                        ) { Text(label) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: Int,
    val body: Int
)
