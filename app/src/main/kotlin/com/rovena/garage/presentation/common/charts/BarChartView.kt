package com.rovena.garage.presentation.common.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rovena.garage.R

/** Minimal, dependency-free vertical bar chart for monthly spend/consumption trends. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Bar(val label: String, val value: Float)

    var bars: List<Bar> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_accent)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_on_surface_variant)
        textAlign = Paint.Align.CENTER
        textSize = 11f * resources.displayMetrics.scaledDensity
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_on_surface)
        textAlign = Paint.Align.CENTER
        textSize = 11f * resources.displayMetrics.scaledDensity
        isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return
        val maxValue = (bars.maxOf { it.value }).coerceAtLeast(1f)
        val labelHeight = labelPaint.textSize + 8f
        val valueHeight = valuePaint.textSize + 8f
        val chartTop = valueHeight
        val chartBottom = height - labelHeight
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

        val slotWidth = width.toFloat() / bars.size
        val barWidth = slotWidth * 0.5f

        bars.forEachIndexed { index, bar ->
            val centerX = slotWidth * index + slotWidth / 2f
            val barHeight = (bar.value / maxValue) * chartHeight
            val top = chartBottom - barHeight
            canvas.drawRoundRect(centerX - barWidth / 2f, top, centerX + barWidth / 2f, chartBottom, 8f, 8f, barPaint)
            if (bar.value > 0) {
                canvas.drawText(formatValue(bar.value), centerX, top - 6f, valuePaint)
            }
            canvas.drawText(bar.label, centerX, height.toFloat(), labelPaint)
        }
    }

    private fun formatValue(value: Float): String =
        if (value >= 1000) String.format("%.1fk", value / 1000f) else value.toInt().toString()
}
