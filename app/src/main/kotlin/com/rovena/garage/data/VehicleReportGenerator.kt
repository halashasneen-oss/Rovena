package com.rovena.garage.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.rovena.garage.R
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object VehicleReportGenerator {
    fun createPdf(
        context: Context,
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceEntity>,
        fuel: List<FuelEntryEntity>,
        expenses: List<ExpenseEntity>,
        documents: List<DocumentEntity>
    ): File {
        val document = PdfDocument()
        val writer = ReportWriter(document)
        val money = { value: Double -> formatMoney(value, vehicle.currencyCode) }
        val totalSpend = maintenance.sumOf { it.cost } + fuel.sumOf { it.totalCost } + expenses.sumOf { it.amount }
        val fuelInsights = FuelAnalytics.analyze(fuel)

        writer.title(context.getString(R.string.report_title))
        writer.text("${vehicle.make} ${vehicle.model} • ${vehicle.year}", 16f, true)
        vehicle.nickname.takeIf { it.isNotBlank() }?.let { writer.text(it, 12f) }
        writer.gap(8f)
        writer.keyValue(context.getString(R.string.report_odometer), "${NumberFormat.getIntegerInstance().format(vehicle.mileage)} km")
        vehicle.plateNumber.takeIf { it.isNotBlank() }?.let { writer.keyValue(context.getString(R.string.report_plate), it) }
        vehicle.vin.takeIf { it.isNotBlank() }?.let { writer.keyValue("VIN", it) }
        writer.keyValue(context.getString(R.string.report_total_spend), money(totalSpend))
        fuelInsights.averageKmPerLiter?.let {
            writer.keyValue(context.getString(R.string.report_avg_efficiency), "${formatNumber(it, 1)} km/L")
        }

        writer.section(context.getString(R.string.maintenance))
        if (maintenance.isEmpty()) writer.muted(context.getString(R.string.report_no_records))
        maintenance.sortedByDescending { it.performedAt }.forEach { item ->
            writer.record(
                item.serviceType,
                "${formatDate(item.performedAt)} • ${NumberFormat.getIntegerInstance().format(item.mileage)} km • ${money(item.cost)}",
                item.notes,
                item.attachmentUri != null,
                context
            )
        }

        writer.section(context.getString(R.string.fuel))
        if (fuel.isEmpty()) writer.muted(context.getString(R.string.report_no_records))
        fuel.sortedByDescending { it.filledAt }.forEach { item ->
            writer.record(
                "${formatNumber(item.liters, 2)} L",
                "${formatDate(item.filledAt)} • ${NumberFormat.getIntegerInstance().format(item.mileage)} km • ${money(item.totalCost)}",
                item.notes,
                item.attachmentUri != null,
                context
            )
        }

        writer.section(context.getString(R.string.expenses))
        if (expenses.isEmpty()) writer.muted(context.getString(R.string.report_no_records))
        expenses.sortedByDescending { it.spentAt }.forEach { item ->
            writer.record(
                item.category,
                "${formatDate(item.spentAt)} • ${money(item.amount)}",
                item.notes,
                item.attachmentUri != null,
                context
            )
        }

        writer.section(context.getString(R.string.documents))
        if (documents.isEmpty()) writer.muted(context.getString(R.string.report_no_records))
        documents.sortedByDescending { it.createdAt }.forEach { item ->
            val expiry = item.expiryAt?.let { context.getString(R.string.expires_on, formatDate(it)) }
                ?: context.getString(R.string.no_expiry)
            writer.record(
                item.title,
                "${item.category} • $expiry",
                item.notes,
                item.fileUri != null,
                context
            )
        }

        writer.section(context.getString(R.string.report_note_title))
        writer.muted(context.getString(R.string.report_note_body))
        writer.finish()

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = listOf(vehicle.make, vehicle.model, vehicle.year.toString())
            .joinToString("-")
            .replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('-')
            .ifBlank { "vehicle" }
        val file = File(dir, "Rovena-$safeName.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_share_title)))
    }

    private fun formatDate(epochMillis: Long): String = runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
    }.getOrDefault("")

    private fun formatMoney(amount: Double, currencyCode: String): String = runCatching {
        NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }.format(amount)
    }.getOrElse { "${formatNumber(amount, 2)} $currencyCode" }

    private fun formatNumber(value: Double, digits: Int): String = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = digits
    }.format(value)

    private class ReportWriter(private val document: PdfDocument) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(25, 30, 38)
        }
        private var page: PdfDocument.Page? = null
        private var y = TOP
        private var pageNumber = 0

        fun title(value: String) {
            ensureSpace(56f)
            text(value, 24f, true)
            muted("Rovena • ${formatDate(System.currentTimeMillis())}")
            gap(12f)
        }

        fun section(value: String) {
            ensureSpace(50f)
            gap(12f)
            text(value, 17f, true)
            gap(5f)
        }

        fun keyValue(key: String, value: String) {
            text("$key: $value", 11f)
        }

        fun record(title: String, detail: String, notes: String, hasAttachment: Boolean, context: Context) {
            ensureSpace(54f)
            text(title, 12f, true)
            text(detail, 10f)
            if (notes.isNotBlank()) muted(notes)
            if (hasAttachment) muted(context.getString(R.string.report_attachment_present))
            gap(7f)
        }

        fun muted(value: String) {
            paint.color = android.graphics.Color.rgb(95, 101, 111)
            text(value, 9.5f)
            paint.color = android.graphics.Color.rgb(25, 30, 38)
        }

        fun text(value: String, size: Float = 11f, bold: Boolean = false) {
            paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            paint.textLocale = Locale.getDefault()
            val lineHeight = size * 1.45f
            wrap(value, paint, CONTENT_WIDTH).forEach { line ->
                ensureSpace(lineHeight + 3f)
                currentCanvas().drawText(line, LEFT, y, paint)
                y += lineHeight
            }
        }

        fun gap(amount: Float) {
            ensureSpace(amount)
            y += amount
        }

        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
        }

        private fun currentCanvas() = requireNotNull(page ?: startPage()).canvas

        private fun ensureSpace(required: Float) {
            if (page == null) startPage()
            if (y + required > BOTTOM) {
                page?.let { document.finishPage(it) }
                page = null
                startPage()
            }
        }

        private fun startPage(): PdfDocument.Page {
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = document.startPage(info)
            page = newPage
            y = TOP
            return newPage
        }

        private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val result = mutableListOf<String>()
            text.lines().forEach { paragraph ->
                if (paragraph.isBlank()) {
                    result += ""
                    return@forEach
                }
                var current = ""
                paragraph.split(Regex("\\s+")).forEach { word ->
                    val candidate = if (current.isBlank()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = candidate
                    } else {
                        if (current.isNotBlank()) result += current
                        current = word
                    }
                }
                if (current.isNotBlank()) result += current
            }
            return result.ifEmpty { listOf(text) }
        }

        companion object {
            private const val PAGE_WIDTH = 595
            private const val PAGE_HEIGHT = 842
            private const val LEFT = 38f
            private const val TOP = 46f
            private const val BOTTOM = 800f
            private const val CONTENT_WIDTH = 519f
        }
    }
}
