package com.rovena.garage.presentation.common.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rovena.garage.R

/** Minimal, dependency-free line chart for fuel-consumption trend over fill-ups. */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_accent)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_accent)
        alpha = 40
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_accent)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.rovena_outline)
        strokeWidth = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        val padding = 16f
        val min = values.min()
        val max = values.max().coerceAtLeast(min + 0.01f)
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        canvas.drawLine(padding, height - padding, width - padding, height - padding, gridPaint)

        val stepX = chartWidth / (values.size - 1)
        val path = Path()
        val fillPath = Path()
        values.forEachIndexed { index, v ->
            val x = padding + stepX * index
            val normalized = (v - min) / (max - min)
            val y = padding + chartHeight - (normalized * chartHeight)
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - padding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            canvas.drawCircle(x, y, 5f, dotPaint)
        }
        fillPath.lineTo(padding + stepX * (values.size - 1), height - padding)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
