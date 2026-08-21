package com.rovena.garage.utils.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal local PDF report builder on top of Android's built-in
 * [PdfDocument] (no external library, no internet - spec #33). Handles page
 * breaks automatically as content is appended; every report generator uses
 * this same layout primitive set for a consistent look.
 */
class PdfBuilder(private val pageWidth: Int = 595, private val pageHeight: Int = 842) {

    private val document = PdfDocument()
    private var page: PdfDocument.Page = newPage()
    private var canvas: Canvas = page.canvas
    private var y = MARGIN

    private val titlePaint = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
    private val headerPaint = Paint().apply { color = Color.BLACK; textSize = 15f; isFakeBoldText = true }
    private val bodyPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f }
    private val captionPaint = Paint().apply { color = Color.GRAY; textSize = 9f }
    private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
    private val accentPaint = Paint().apply { color = Color.parseColor("#FF6A1A"); textSize = 11f; isFakeBoldText = true }

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

    fun title(text: String) {
        ensureSpace(30f)
        canvas.drawText(text, MARGIN, y, titlePaint)
        y += 30f
    }

    fun caption(text: String) {
        ensureSpace(16f)
        canvas.drawText(text, MARGIN, y, captionPaint)
        y += 16f
    }

    fun sectionHeader(text: String) {
        ensureSpace(28f)
        y += 8f
        canvas.drawText(text, MARGIN, y, headerPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, pageWidth - MARGIN, y, linePaint)
        y += 14f
    }

    fun bodyLine(text: String) {
        ensureSpace(16f)
        canvas.drawText(text, MARGIN, y, bodyPaint)
        y += 16f
    }

    fun keyValueRow(label: String, value: String, valueAccent: Boolean = false) {
        ensureSpace(18f)
        canvas.drawText(label, MARGIN, y, bodyPaint)
        val paint = if (valueAccent) accentPaint else bodyPaint
        canvas.drawText(value, pageWidth - MARGIN - paint.measureText(value), y, paint)
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
