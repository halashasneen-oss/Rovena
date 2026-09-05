package com.rovena.garage.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rovena.garage.R
import com.rovena.garage.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RovenaRoot(
    preferences: AppPreferences,
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
                CoroutineScope(Dispatchers.Main).launch {
                    preferences.completeOnboarding()
                    onRequestNotifications()
                }
            }
        )
    } else {
        MainShell()
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
            onClick = {
                if (page < 3) page++ else onFinished()
            },
            enabled = page != 0 || selectedLanguage.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (page < 3) stringResource(R.string.continue_label) else stringResource(R.string.get_started))
        }
    }
}

private enum class MainTab { HOME, CAR, HISTORY, EXPENSES, MORE }

@Composable
private fun MainShell() {
    var tab by rememberSaveable { androidx.compose.runtime.mutableStateOf(MainTab.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == MainTab.HOME, { tab = MainTab.HOME }, { Icon(Icons.Rounded.Home, null) }, { Text(stringResource(R.string.home)) })
                NavigationBarItem(tab == MainTab.CAR, { tab = MainTab.CAR }, { Icon(Icons.Rounded.DirectionsCar, null) }, { Text(stringResource(R.string.my_car)) })
                NavigationBarItem(tab == MainTab.HISTORY, { tab = MainTab.HISTORY }, { Icon(Icons.Rounded.History, null) }, { Text(stringResource(R.string.history)) })
                NavigationBarItem(tab == MainTab.EXPENSES, { tab = MainTab.EXPENSES }, { Icon(Icons.Rounded.Payments, null) }, { Text(stringResource(R.string.expenses)) })
                NavigationBarItem(tab == MainTab.MORE, { tab = MainTab.MORE }, { Icon(Icons.Rounded.MoreHoriz, null) }, { Text(stringResource(R.string.more)) })
            }
        }
    ) { padding ->
        when (tab) {
            MainTab.HOME -> Dashboard(Modifier.padding(padding))
            MainTab.CAR -> PlaceholderScreen(R.string.my_car, Modifier.padding(padding))
            MainTab.HISTORY -> PlaceholderScreen(R.string.history, Modifier.padding(padding))
            MainTab.EXPENSES -> PlaceholderScreen(R.string.expenses, Modifier.padding(padding))
            MainTab.MORE -> PlaceholderScreen(R.string.more, Modifier.padding(padding))
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.dashboard_greeting), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.dashboard_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.no_vehicle_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.no_vehicle_body))
                    Button(onClick = { }) {
                        Icon(Icons.Rounded.Add, null)
                        Text("  ${stringResource(R.string.add_vehicle)}")
                    }
                }
            }
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
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(stringResource(R.string.coming_next), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.foundation_message))
                }
            }
        }
    }
}

@Composable
private fun QuickCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(title), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}
