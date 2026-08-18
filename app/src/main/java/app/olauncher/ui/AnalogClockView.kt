package app.olauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import app.olauncher.R
import app.olauncher.helper.getColorFromAttr
import java.util.Calendar
import java.util.Date
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Minimal analog clock for the home screen: a thin ring, four quarter ticks, an hour hand and a
 * minute hand, all drawn in the theme's primary colour (light/dark aware). Redraws on the minute
 * and whenever its window becomes visible again, so it is correct after the screen was off.
 */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val primary = context.getColorFromAttr(R.attr.primaryColor)
    private val density = resources.displayMetrics.density

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = primary
        alpha = 150
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        strokeCap = Paint.Cap.ROUND
        color = primary
        alpha = 190
    }
    private val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
        color = primary
    }
    private val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
        color = primary
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primary
    }

    private val uiHandler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            scheduleNextMinute()
        }
    }

    /** Redraw now and update the accessibility description. */
    fun refresh() {
        contentDescription = DateFormat.getTimeFormat(context).format(Date())
        invalidate()
    }

    private fun scheduleNextMinute() {
        uiHandler.removeCallbacks(ticker)
        val now = System.currentTimeMillis()
        val delay = MINUTE_MS - (now % MINUTE_MS) + 50L
        uiHandler.postDelayed(ticker, delay)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refresh()
        scheduleNextMinute()
    }

    override fun onDetachedFromWindow() {
        uiHandler.removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        uiHandler.removeCallbacks(ticker)
        if (visibility == VISIBLE) {
            refresh()
            scheduleNextMinute()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (DEFAULT_SIZE_DP * density).toInt()
        val w = resolveSize(desired + paddingLeft + paddingRight, widthMeasureSpec)
        val h = resolveSize(desired + paddingTop + paddingBottom, heightMeasureSpec)
        val size = min(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = (paddingLeft + (width - paddingLeft - paddingRight) / 2).toFloat()
        val cy = (paddingTop + (height - paddingTop - paddingBottom) / 2).toFloat()
        val radius = min(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom) / 2f -
                ringPaint.strokeWidth

        canvas.drawCircle(cx, cy, radius, ringPaint)

        val tickLength = radius * 0.1f
        for (i in 0 until 4) {
            val angle = Math.toRadians(i * 90.0)
            val sx = cx + (sin(angle) * (radius - tickLength)).toFloat()
            val sy = cy - (cos(angle) * (radius - tickLength)).toFloat()
            val ex = cx + (sin(angle) * radius).toFloat()
            val ey = cy - (cos(angle) * radius).toFloat()
            canvas.drawLine(sx, sy, ex, ey, tickPaint)
        }

        val now = Calendar.getInstance()
        val minute = now.get(Calendar.MINUTE)
        val hour = now.get(Calendar.HOUR)
        val minuteAngle = Math.toRadians(minute * 6.0)
        val hourAngle = Math.toRadians((hour + minute / 60.0) * 30.0)

        drawHand(canvas, cx, cy, hourAngle, radius * 0.52f, hourPaint)
        drawHand(canvas, cx, cy, minuteAngle, radius * 0.8f, minutePaint)
        canvas.drawCircle(cx, cy, hourPaint.strokeWidth * 0.9f, centerPaint)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, angle: Double, length: Float, paint: Paint) {
        val ex = cx + (sin(angle) * length).toFloat()
        val ey = cy - (cos(angle) * length).toFloat()
        canvas.drawLine(cx, cy, ex, ey, paint)
    }

    companion object {
        private const val DEFAULT_SIZE_DP = 96f
        private const val MINUTE_MS = 60_000L
    }
}
