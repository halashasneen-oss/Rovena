package com.rovena.garage.utils.pdf

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import com.rovena.garage.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * A PDF report is a formal document that may leave the app entirely (shared
 * with a workshop, insurer, or buyer), so its numbers are always rendered in
 * Western digits even when the active locale is Arabic - the standard
 * convention for Arabic-language business/technical documents - while
 * everything else (month names, translated labels) stays fully localized.
 * Only the Unicode numbering-system extension changes here, so resource
 * lookups (`values-ar/`) and RTL layout direction are unaffected.
 */
fun Context.withWesternNumerals(): Context {
    val locale = Locale.Builder().setLocale(resources.configuration.locales[0]).setUnicodeLocaleKeyword("nu", "latn").build()
    val config = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(config)
}

/**
 * Minimal local PDF report builder on top of Android's built-in
 * [PdfDocument] (no external library, no internet - spec #33). Handles page
 * breaks automatically as content is appended; every report generator uses
 * this same layout primitive set for a consistent look.
 *
 * Every line is measured and drawn through [StaticLayout] rather than raw
 * [Canvas.drawText] - plain `drawText` neither wraps long text (it silently
 * runs off the page edge) nor reorders mixed-direction runs (an Arabic label
 * next to a Western-digit number), both of which StaticLayout's bidi/line-
 * breaking engine handles correctly. [isRtl] additionally mirrors which
 * margin each block starts from, and which side of [keyValueRow] the label
 * vs. value sits on, so an Arabic-locale report reads right-to-left overall
 * rather than just containing correctly-shaped RTL text at a left margin.
 */
class PdfBuilder(context: Context, private val pageWidth: Int = 595, private val pageHeight: Int = 842) {

    private val isRtl: Boolean = TextUtils.getLayoutDirectionFromLocale(context.resources.configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL
    private val textDirection = if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR

    private val document = PdfDocument()
    private var page: PdfDocument.Page = newPage()
    private var canvas: Canvas = page.canvas
    private var y = MARGIN

    private val titlePaint = TextPaint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
    private val headerPaint = TextPaint().apply { color = Color.BLACK; textSize = 15f; isFakeBoldText = true }
    private val bodyPaint = TextPaint().apply { color = Color.DKGRAY; textSize = 11f }
    private val captionPaint = TextPaint().apply { color = Color.GRAY; textSize = 9f }
    private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
    private val accentPaint = TextPaint().apply { color = ContextCompat.getColor(context, R.color.rovena_accent); textSize = 11f; isFakeBoldText = true }

    private fun newPage(): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
        return document.startPage(info)
    }

    private fun ensureSpace(needed: Float) {
        if (y + needed > pageHeight - MARGIN) {
            document.finishPage(page)
            page = newPage()
            canvas = page.canvas
            y = MARGIN
        }
    }

    private fun contentWidth(): Int = (pageWidth - 2 * MARGIN).toInt()

    /** A single- or multi-line block, wrapped to the full content width and drawn starting at the current [y]. */
    private fun drawBlock(text: String, paint: TextPaint, bottomPadding: Float) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(textDirection)
            .setIncludePad(false)
            .build()
        val height = layout.height.toFloat()
        ensureSpace(height + bottomPadding)
        canvas.save()
        canvas.translate(MARGIN, y)
        layout.draw(canvas)
        canvas.restore()
        y += height + bottomPadding
    }

    /** A short, single-line run measured to its own natural width (no wrapping) - used for [keyValueRow]'s two halves. */
    private fun drawInline(text: String, paint: TextPaint, x: Float): Float {
        val width = kotlin.math.ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(textDirection)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    fun title(text: String) {
        drawBlock(text, titlePaint, 8f)
    }

    fun caption(text: String) {
        drawBlock(text, captionPaint, 5f)
    }

    fun sectionHeader(text: String) {
        y += 8f
        drawBlock(text, headerPaint, 6f)
        canvas.drawLine(MARGIN, y, pageWidth - MARGIN, y, linePaint)
        y += 14f
    }

    fun bodyLine(text: String) {
        drawBlock(text, bodyPaint, 5f)
    }

    fun keyValueRow(label: String, value: String, valueAccent: Boolean = false) {
        val valuePaint = if (valueAccent) accentPaint else bodyPaint
        ensureSpace(18f)
        val labelWidth = bodyPaint.measureText(label)
        val valueWidth = valuePaint.measureText(value)
        if (isRtl) {
            drawInline(value, valuePaint, MARGIN)
            drawInline(label, bodyPaint, pageWidth - MARGIN - labelWidth)
        } else {
            drawInline(label, bodyPaint, MARGIN)
            drawInline(value, valuePaint, pageWidth - MARGIN - valueWidth)
        }
        y += 18f
    }

    fun divider() {
        ensureSpace(10f)
        canvas.drawLine(MARGIN, y, pageWidth - MARGIN, y, linePaint)
        y += 10f
    }

    fun spacer(height: Float = 8f) {
        ensureSpace(height)
        y += height
    }

    fun save(outputFile: File) {
        document.finishPage(page)
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
    }

    companion object {
        private const val MARGIN = 40f
    }
}
