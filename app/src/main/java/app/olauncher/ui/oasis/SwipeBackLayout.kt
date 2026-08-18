package app.olauncher.ui.oasis

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * FrameLayout that recognises a horizontal swipe made anywhere over its content, even while a
 * child (ScrollView, EditText, buttons) is handling the touch. Vertical scrolling is left to the
 * children; once a drag becomes clearly horizontal the gesture is taken over (the child receives
 * ACTION_CANCEL) and the matching callback fires on release.
 */
class SwipeBackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minSwipeDistance = SWIPE_DP * resources.displayMetrics.density

    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var stolen = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                tracking = true
                stolen = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (tracking && !stolen) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (abs(dx) > touchSlop * 2 && abs(dx) > abs(dy) * 2) {
                        stolen = true
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tracking = false
                stolen = false
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                tracking = true
                stolen = false
            }

            MotionEvent.ACTION_UP -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (tracking && abs(dx) >= minSwipeDistance && abs(dx) > abs(dy)) {
                    if (dx < 0) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
                }
                tracking = false
                stolen = false
            }

            MotionEvent.ACTION_CANCEL -> {
                tracking = false
                stolen = false
            }
        }
        return true
    }

    companion object {
        private const val SWIPE_DP = 72f
    }
}
