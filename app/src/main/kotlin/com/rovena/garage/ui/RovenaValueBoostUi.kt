package com.rovena.garage.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rovena.garage.R
import com.rovena.garage.data.MonthlyCostInsights
import com.rovena.garage.data.UpcomingItem
import com.rovena.garage.data.UpcomingKind
import com.rovena.garage.data.UpcomingUrgency
import com.rovena.garage.data.VehicleHealthStatus
import com.rovena.garage.data.VehicleHealthSummary
import com.rovena.garage.ui.theme.RovenaPalette
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** A data-based care summary, never a mechanical diagnosis. */
@Composable
internal fun RovenaCareSpotlight(
    health: VehicleHealthSummary,
    upcoming: List<UpcomingItem>,
    fuelDrop: Boolean,
    onAddRecord: (RecordAction) -> Unit
) {
    val score = health.score
    val accent = when (health.status) {
        VehicleHealthStatus.URGENT -> RovenaPalette.Danger
        VehicleHealthStatus.ATTENTION -> RovenaPalette.Warning
        else -> RovenaPalette.Cyan
    }
    val status = when (health.status) {
        VehicleHealthStatus.EXCELLENT -> R.string.vb_excellent
        VehicleHealthStatus.GOOD -> R.string.vb_good
        VehicleHealthStatus.ATTENTION -> R.string.vb_attention
        VehicleHealthStatus.URGENT -> R.string.vb_urgent
        VehicleHealthStatus.UNKNOWN -> R.string.vb_not_enough_data
    }
    val priorities = upcoming.filter { it.urgency != UpcomingUrgency.LATER }.take(3)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF0C3549), Color(0xFF071926), Color(0xFF0B2735))))
                .padding(19.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text(stringResource(R.string.vb_vehicle_health), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(19.dp)) {
                    Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (score ?: 0).coerceIn(0, 100) / 100f },
                            modifier = Modifier.size(110.dp),
                            strokeWidth = 9.dp,
                            color = accent,
                            trackColor = RovenaPalette.SurfaceSoft,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(score?.toString() ?: "—", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                            Text("/100", color = RovenaPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(status), color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            stringResource(R.string.vb_confidence) + ": " + when (health.confidence) {
                                com.rovena.garage.data.HealthConfidence.HIGH -> stringResource(R.string.vb_high)
                                com.rovena.garage.data.HealthConfidence.MEDIUM -> stringResource(R.string.vb_medium)
                                com.rovena.garage.data.HealthConfidence.LOW -> stringResource(R.string.vb_low)
                            },
                            color = RovenaPalette.TextSecondary
                        )
                        Text(stringResource(R.string.vb_not_diagnostic), style = MaterialTheme.typography.bodySmall, color = RovenaPalette.TextSecondary)
                    }
                }
                Spacer(Modifier.height(1.dp))
                Text(stringResource(R.string.vb_top_priorities), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (priorities.isEmpty() && !fuelDrop) {
                    Text(
                        stringResource(if (score == null) R.string.vb_add_records else R.string.vb_no_priority),
                        color = RovenaPalette.TextSecondary
                    )
                }
                priorities.forEach { item ->
                    val action = if (item.kind == UpcomingKind.MAINTENANCE) RecordAction.MAINTENANCE else RecordAction.DOCUMENT
                    val itemColor = if (item.urgency == UpcomingUrgency.OVERDUE) RovenaPalette.Danger else RovenaPalette.Warning
                    PriorityAction(item.title, if (item.urgency == UpcomingUrgency.OVERDUE) R.string.vb_overdue else R.string.vb_due_soon, itemColor) {
                        onAddRecord(action)
                    }
                }
                if (fuelDrop && priorities.size < 3) {
                    PriorityAction(stringResource(R.string.vb_fuel_drop), R.string.vb_review_fuel, RovenaPalette.Warning) {
                        onAddRecord(RecordAction.FUEL)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityAction(title: String, labelRes: Int, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = RovenaPalette.SurfaceRaised.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Rounded.Assignment, null, tint = accent, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(stringResource(labelRes), color = accent, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Rounded.ArrowForward, null, tint = RovenaPalette.Cyan, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun RovenaExpenseRadar(
    analytics: MonthlyCostInsights,
    lifetimeSpend: Double,
    currencyCode: String
) {
    val top = maxOf(analytics.current.total, analytics.previous.total, 1.0)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.33f))
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF0B3043), Color(0xFF081A2A))))
                .padding(19.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.vb_expense_radar), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.vb_lifetime_spend), color = RovenaPalette.TextSecondary)
            Text(vbMoney(lifetimeSpend, currencyCode), color = RovenaPalette.Cyan, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            RadarBar(stringResource(R.string.vb_current_month), analytics.current.total, top, currencyCode, RovenaPalette.Cyan)
            RadarBar(stringResource(R.string.vb_previous_month), analytics.previous.total, top, currencyCode, RovenaPalette.Sky)
            Text(
                if (analytics.changePercent == null) stringResource(R.string.vb_no_previous_comparison)
                else stringResource(R.string.vb_month_change, analytics.changePercent),
                color = if ((analytics.changePercent ?: 0.0) > 0.0) RovenaPalette.Warning else RovenaPalette.Success,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadarPill(Modifier.weight(1f), Icons.Rounded.LocalGasStation, stringResource(R.string.fuel), vbMoney(analytics.current.fuel, currencyCode))
                RadarPill(Modifier.weight(1f), Icons.Rounded.Build, stringResource(R.string.maintenance), vbMoney(analytics.current.maintenance, currencyCode))
                RadarPill(Modifier.weight(1f), Icons.Rounded.Payments, stringResource(R.string.vb_other), vbMoney(analytics.current.other, currencyCode))
            }
            Text(
                stringResource(R.string.vb_cost_per_km) + ": " +
                    (analytics.costPer100Km?.let { vbMoney(it / 100.0, currencyCode) } ?: stringResource(R.string.vb_not_enough_data)),
                color = RovenaPalette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RadarBar(label: String, amount: Double, max: Double, currencyCode: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(vbMoney(amount, currencyCode), fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(RovenaPalette.SurfaceSoft)) {
            Box(
                Modifier.fillMaxWidth((amount / max).toFloat().coerceIn(0f, 1f))
                    .height(9.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(RovenaPalette.Accent, accent)))
            )
        }
    }
}

@Composable
private fun RadarPill(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = RovenaPalette.SurfaceRaised) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = RovenaPalette.Cyan, modifier = Modifier.size(20.dp))
            Text(label, color = RovenaPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
internal fun RovenaPassportCard(onShareReport: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onShareReport),
        shape = RoundedCornerShape(24.dp),
        color = RovenaPalette.SurfaceRaised,
        border = BorderStroke(1.dp, RovenaPalette.Cyan.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = RovenaPalette.Accent.copy(alpha = 0.16f)) {
                Icon(Icons.Rounded.Description, null, modifier = Modifier.padding(12.dp), tint = RovenaPalette.Cyan)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.vb_passport), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.vb_passport_subtitle), style = MaterialTheme.typography.bodySmall, color = RovenaPalette.TextSecondary)
            }
            Icon(Icons.Rounded.ArrowForward, null, tint = RovenaPalette.Cyan)
        }
    }
}

private fun vbMoney(value: Double, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(value)
}.getOrElse { String.format(Locale.getDefault(), "%.2f %s", value, currencyCode) }
