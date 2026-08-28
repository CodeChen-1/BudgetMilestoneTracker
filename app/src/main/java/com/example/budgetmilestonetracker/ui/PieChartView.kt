package com.example.budgetmilestonetracker.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

// A simple pie chart that draws category spending as coloured slices.
// It adapts its background to the current night mode automatically.
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Each slice is a triple of (name, amount, colour).
    data class Slice(val name: String, val amount: Double, val color: Int)

    private var slices: List<Slice> = emptyList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // The "No data" message – can be overridden for translations
    private var noDataText: String = "No data"

    fun setData(data: List<Slice>) {
        slices = data
        invalidate()
    }

    // Let the fragment provide a translated string for the empty state
    fun setNoDataText(text: String) {
        noDataText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (slices.isEmpty() || slices.sumOf { it.amount } == 0.0) {
            // Draw a simple "No data" circle
            paint.style = Paint.Style.FILL
            paint.color = Color.LTGRAY
            val cx = width / 2f
            val cy = height / 2f
            val radius = (minOf(width, height) / 2f) * 0.8f
            canvas.drawCircle(cx, cy, radius, paint)
            textPaint.color = Color.DKGRAY
            canvas.drawText(noDataText, cx, cy + 8, textPaint)
            return
        }

        val total = slices.sumOf { it.amount }
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) * 0.8f
        var startAngle = -90f   // start from top

        // Choose text colour based on night mode
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        textPaint.color = if (isDark) Color.WHITE else Color.BLACK

        for (slice in slices) {
            val sweep = ((slice.amount / total * 360).toFloat()).coerceAtLeast(1f)

            // Draw the slice
            paint.style = Paint.Style.FILL
            paint.color = slice.color
            canvas.drawArc(
                cx - radius, cy - radius, cx + radius, cy + radius,
                startAngle, sweep, true, paint
            )

            // Draw a small label at the centre of the slice
            val midAngle = startAngle + sweep / 2f
            val midRad = Math.toRadians(midAngle.toDouble())
            val labelRadius = radius * 0.65f
            val lx = cx + labelRadius * cos(midRad).toFloat()
            val ly = cy + labelRadius * sin(midRad).toFloat()
            val pct = ((slice.amount / total) * 100).toInt()
            canvas.drawText("${slice.name} $pct%", lx, ly, textPaint)

            startAngle += sweep
        }
    }
}