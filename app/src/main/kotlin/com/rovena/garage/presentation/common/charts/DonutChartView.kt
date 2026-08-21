package com.rovena.garage.presentation.common.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Minimal, dependency-free donut chart for category cost breakdowns. */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val value: Float, val color: Int)

    var slices: List<Slice> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        color = Color.parseColor("#22808080")
    }
    private val bounds = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = paint.strokeWidth / 2f + 4f
        bounds.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(bounds, 0f, 360f, false, trackPaint)
        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) return

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total) * 360f
            paint.color = slice.color
            canvas.drawArc(bounds, startAngle, sweep, false, paint)
            startAngle += sweep
        }
    }
}
