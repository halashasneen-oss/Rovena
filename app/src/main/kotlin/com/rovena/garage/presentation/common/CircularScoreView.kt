package com.rovena.garage.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rovena.garage.R

/**
 * Simple canvas-drawn progress ring used for the Vehicle Health Score (and
 * reused for inspection scores). Deliberately hand-rolled instead of pulling
 * in a chart library - one ring doesn't need a whole dependency.
 */
class CircularScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        color = ContextCompat.getColor(context, R.color.rovena_surface_alt)
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.rovena_status_good)
    }

    private val bounds = RectF()

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var ringColor: Int = progressPaint.color
        set(value) {
            field = value
            progressPaint.color = value
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeInset = trackPaint.strokeWidth / 2f + paddingLeft
        bounds.set(strokeInset, strokeInset, w - strokeInset, h - strokeInset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(bounds, -90f, 360f, false, trackPaint)
        val sweep = 360f * (progress / 100f)
        canvas.drawArc(bounds, -90f, sweep, false, progressPaint)
    }
}
