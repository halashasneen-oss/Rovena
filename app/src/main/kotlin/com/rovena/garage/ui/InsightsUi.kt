package com.rovena.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.FuelAnalytics
import com.rovena.garage.data.FuelInsights
import com.rovena.garage.data.FuelTrend
import com.rovena.garage.data.HealthConfidence
import com.rovena.garage.data.VehicleHealthEngine
import com.rovena.garage.data.VehicleHealthStatus
import com.rovena.garage.data.VehicleHealthSummary
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun SmartHubScreen(
    vehicle: VehicleEntity?,
    maintenance: List<MaintenanceEntity>,
    fuel: List<FuelEntryEntity>,
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
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.insights_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (vehicle == null) {
            item { InsightEmptyVehicle() }
        } else {
            val health = VehicleHealthEngine.evaluate(vehicle, maintenance, documents, fuel)
            val fuelInsights = FuelAnalytics.analyze(fuel)

            item { HealthScoreCard(health) }
            item { FuelInsightsCard(fuelInsights, vehicle.currencyCode) }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.documents), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.smart_documents_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = onAddDocument) {
                        Icon(Icons.Rounded.Add, null)
                        Text("  ${stringResource(R.string.add_document)}")
                    }
                }
            }

            if (documents.isEmpty()) {
                item { SimpleInfoCard(R.string.no_documents_title, R.string.no_documents_body) }
            } else {
                items(documents.take(5), key = { it.id }) { document -> InsightDocumentCard(document) }
            }

            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(R.string.health_disclaimer_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.health_disclaimer), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthScoreCard(summary: VehicleHealthSummary) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        Icons.Rounded.HealthAndSafety,
                        null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.care_score), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(healthStatusLabel(summary.status), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(
                    summary.score?.let { "$it/100" } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                stringResource(R.string.data_confidence, confidenceLabel(summary.confidence)),
                style = MaterialTheme.typography.labelLarge
            )

            if (summary.score == null) {
                Text(stringResource(R.string.care_score_unavailable), color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else if (summary.attentionCount == 0) {
                Text(stringResource(R.string.health_no_attention), fontWeight = FontWeight.SemiBold)
            } else {
                Text(
                    stringResource(
                        R.string.health_attention_counts,
                        summary.overdueMaintenance,
                        summary.dueSoonMaintenance,
                        summary.expiredDocuments,
                        summary.expiringDocuments
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun FuelInsightsCard(insights: FuelInsights, currencyCode: String) {
    Card(shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(stringResource(R.string.fuel_insights_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.fuel_insights_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (insights.intervalCount == 0) {
                Text(stringResource(R.string.fuel_not_enough_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsightMetric(
                        Modifier.weight(1f),
                        stringResource(R.string.fuel_efficiency),
                        "${formatInsightNumber(insights.averageKmPerLiter)} km/L"
                    )
                    InsightMetric(
                        Modifier.weight(1f),
                        stringResource(R.string.fuel_l_per_100),
                        "${formatInsightNumber(insights.litersPer100Km)} L/100 km"
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsightMetric(
                        Modifier.weight(1f),
                        stringResource(R.string.fuel_cost_per_km),
                        "${formatInsightNumber(insights.averageFuelCostPerKm, 3)} $currencyCode/km"
                    )
                    InsightMetric(
                        Modifier.weight(1f),
                        stringResource(R.string.fuel_price_per_liter),
                        "${formatInsightNumber(insights.averagePricePerLiter, 3)} $currencyCode/L"
                    )
                }

                val trendText = if (insights.trendPercent == null) {
                    fuelTrendLabel(insights.trend)
                } else {
                    stringResource(
                        R.string.fuel_trend_change,
                        fuelTrendLabel(insights.trend),
                        abs(insights.trendPercent).toInt()
                    )
                }
                Text(trendText, fontWeight = FontWeight.Bold, color = trendColor(insights.trend))
            }

            Text(
                stringResource(R.string.fuel_method_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightMetric(modifier: Modifier, label: String, value: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InsightDocumentCard(document: DocumentEntity) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(document.title, fontWeight = FontWeight.Bold)
                Text(document.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    document.expiryAt?.let { stringResource(R.string.expires_on, formatInsightDate(it)) }
                        ?: stringResource(R.string.no_expiry),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun InsightEmptyVehicle() {
    SimpleInfoCard(R.string.no_vehicle_title, R.string.no_vehicle_body)
}

@Composable
private fun SimpleInfoCard(title: Int, body: Int) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun healthStatusLabel(status: VehicleHealthStatus): String = stringResource(
    when (status) {
        VehicleHealthStatus.EXCELLENT -> R.string.care_status_excellent
        VehicleHealthStatus.GOOD -> R.string.care_status_good
        VehicleHealthStatus.ATTENTION -> R.string.care_status_attention
        VehicleHealthStatus.URGENT -> R.string.care_status_urgent
        VehicleHealthStatus.UNKNOWN -> R.string.care_status_unknown
    }
)

@Composable
private fun confidenceLabel(confidence: HealthConfidence): String = stringResource(
    when (confidence) {
        HealthConfidence.LOW -> R.string.confidence_low
        HealthConfidence.MEDIUM -> R.string.confidence_medium
        HealthConfidence.HIGH -> R.string.confidence_high
    }
)

@Composable
private fun fuelTrendLabel(trend: FuelTrend): String = stringResource(
    when (trend) {
        FuelTrend.IMPROVING -> R.string.fuel_trend_improving
        FuelTrend.STABLE -> R.string.fuel_trend_stable
        FuelTrend.WORSENING -> R.string.fuel_trend_worsening
        FuelTrend.UNKNOWN -> R.string.fuel_trend_unknown
    }
)

@Composable
private fun trendColor(trend: FuelTrend) = when (trend) {
    FuelTrend.WORSENING -> MaterialTheme.colorScheme.error
    FuelTrend.IMPROVING -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatInsightNumber(value: Double?, digits: Int = 1): String {
    if (value == null || !value.isFinite()) return "—"
    return NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = digits
        minimumFractionDigits = 0
    }.format(value)
}

private fun formatInsightDate(epochMillis: Long): String = runCatching {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
}.getOrDefault("")
