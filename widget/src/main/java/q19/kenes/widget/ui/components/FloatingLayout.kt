package q19.kenes.widget.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.card.MaterialCardView
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.core.logging.Logger
import q19.kenes_widget.R

internal class FloatingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = FloatingLayout::class.java.simpleName
    }

    private val touchSlop: Float = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    var leftPadding: Float = 0F
        private set

    var rightPadding: Float = 0F
        private set

    var topPadding: Float = 0F
        private set

    var bottomPadding: Float = 0F
        private set

    private var bottomOffset: Int = 0

    init {
        leftPadding = 16F.dp2Px()
        rightPadding = 16F.dp2Px()
        topPadding = 16F.dp2Px()
        bottomPadding = 16F.dp2Px() + bottomOffset
    }

    var startX: Float = 0F
        private set

    var startY: Float = 0F
        private set

    var startMovingFromX: Float = 0F
        private set

    var startMovingFromY: Float = 0F
        private set

    var isMoving: Boolean = false
        private set

    var isActive: Boolean = true

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isActive) return false

        val parent = parent
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x + x
                startY = event.y + y

                animate().setListener(null).cancel()

                animate()
                    .scaleY(1.1F)
                    .scaleX(1.1F)
                    .alpha(1F)
                    .setStartDelay(0)
                    .start()
            }
            MotionEvent.ACTION_MOVE -> {
                var dx = event.x + x - startX
                var dy = event.y + y - startY

                if (!isMoving && dx * dx + dy * dy > touchSlop * touchSlop) {
                    parent?.requestDisallowInterceptTouchEvent(true)

                    isMoving = true

                    startX = event.x + x
                    startY = event.y + y

                    startMovingFromX = translationX
                    startMovingFromY = translationY

                    dx = 0F
                    dy = 0F
                }

                if (isMoving) {
                    translationX = startMovingFromX + dx
                    translationY = startMovingFromY + dy
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (parent is View) {
                    parent.requestDisallowInterceptTouchEvent(false)

                    animate().setListener(null).cancel()

                    val animator = animate()
                        .scaleX(1F)
                        .scaleY(1F)
                        .alpha(1F)
                        .setStartDelay(0)

                    val parentWidth = parent.measuredWidth
                    val parentHeight = parent.measuredHeight

                    val maxLeft = leftPadding
                    val maxRight = rightPadding
                    val maxTop = topPadding
                    val maxBottom = bottomPadding

                    Logger.debug(TAG, "x: $x, y: $y")
                    Logger.debug(TAG, "maxLeft: $maxLeft, maxRight: $maxRight")
                    Logger.debug(TAG, "maxTop: $maxTop, maxBottom: $maxBottom")
                    Logger.debug(TAG, "parentWidth: $parentWidth, parentHeight: $parentHeight")

                    if (x < maxLeft) {
                        animator.x(maxLeft)
                    } else if (x + measuredWidth > parentWidth - maxRight) {
                        animator.x(parentWidth - measuredWidth - maxRight)
                    }

                    if (y < maxTop) {
                        animator.y(maxTop)
                    } else if (y + measuredHeight > parentHeight - maxBottom) {
                        animator.y(parentHeight - measuredHeight - maxBottom)
                    }

                    animator.setDuration(150)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
                isMoving = false
            }
        }
        return true
    }

    fun getBottomOffset(): Int {
        return bottomOffset
    }

    fun setBottomOffset(bottomOffset: Int, isAnimated: Boolean) {
        val parent = parent
        if (parent == null || !isAnimated) {
            this.bottomOffset = bottomOffset
            return
        }
        this.bottomOffset = bottomOffset
    }

}