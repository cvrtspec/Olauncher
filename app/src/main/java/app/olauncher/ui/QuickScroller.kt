package app.olauncher.ui

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Old-school quick-scroll grip for a vertical RecyclerView: a fixed-size tab on the end edge that
 * appears while the list scrolls, fades after a moment, and can be dragged. Unlike RecyclerView's
 * built-in fast scroller (which maps drag *distance* over the whole view height, so grabbing the
 * middle of the thumb can never reach the end), the tab's position is mapped absolutely: tab at the
 * top = start of the list, tab at the bottom = very end, wherever on the tab the finger lands.
 */
class QuickScroller(
    private val recyclerView: RecyclerView,
    private val thumb: Drawable,
    private val thumbWidth: Int,
    private val thumbHeight: Int,
    private val edgeMargin: Int
) : RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener {

    private var visible = false
    private var dragging = false
    private var thumbTop = 0f
    private var grabOffset = 0f
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        visible = false
        recyclerView.invalidate()
    }

    init {
        recyclerView.addItemDecoration(this)
        recyclerView.addOnItemTouchListener(this)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0 && !dragging) show()
            }
        })
    }

    private fun show() {
        visible = true
        recyclerView.invalidate()
        uiHandler.removeCallbacks(hideRunnable)
        uiHandler.postDelayed(hideRunnable, HIDE_DELAY_MS)
    }

    private fun scrollable(): Boolean =
        recyclerView.computeVerticalScrollRange() > recyclerView.computeVerticalScrollExtent()

    private fun travel(): Int = (recyclerView.height - thumbHeight).coerceAtLeast(1)

    private fun thumbLeft(): Int =
        if (recyclerView.layoutDirection == View.LAYOUT_DIRECTION_RTL) edgeMargin
        else recyclerView.width - edgeMargin - thumbWidth

    private fun updateThumbFromScroll() {
        val range = recyclerView.computeVerticalScrollRange()
        val extent = recyclerView.computeVerticalScrollExtent()
        val offset = recyclerView.computeVerticalScrollOffset()
        val total = (range - extent).coerceAtLeast(1)
        thumbTop = travel() * (offset.toFloat() / total)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!visible || !scrollable()) return
        if (!dragging) updateThumbFromScroll()
        val left = thumbLeft()
        val top = thumbTop.toInt()
        thumb.state = if (dragging) PRESSED_STATE else EMPTY_STATE
        thumb.setBounds(left, top, left + thumbWidth, top + thumbHeight)
        thumb.draw(c)
    }

    private fun hitsThumb(e: MotionEvent): Boolean {
        val left = thumbLeft()
        val slop = thumbWidth / 2
        return e.x >= left - slop && e.x <= left + thumbWidth + slop &&
                e.y >= thumbTop - slop && e.y <= thumbTop + thumbHeight + slop
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN && visible && scrollable() && hitsThumb(e)) {
            dragging = true
            grabOffset = e.y - thumbTop
            uiHandler.removeCallbacks(hideRunnable)
            rv.parent?.requestDisallowInterceptTouchEvent(true)
            rv.invalidate()
            return true
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!dragging) return
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> dragTo(e.y - grabOffset)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                show()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    /** Absolute mapping: thumb top 0 -> list start, thumb top = travel -> list end. */
    private fun dragTo(top: Float) {
        val travel = travel()
        thumbTop = top.coerceIn(0f, travel.toFloat())
        val fraction = thumbTop / travel
        val range = recyclerView.computeVerticalScrollRange()
        val extent = recyclerView.computeVerticalScrollExtent()
        val offset = recyclerView.computeVerticalScrollOffset()
        val target = (fraction * (range - extent)).toInt()
        recyclerView.scrollBy(0, target - offset)
        recyclerView.invalidate()
    }

    companion object {
        private const val HIDE_DELAY_MS = 1500L
        private val PRESSED_STATE = intArrayOf(android.R.attr.state_pressed)
        private val EMPTY_STATE = intArrayOf()
    }
}
