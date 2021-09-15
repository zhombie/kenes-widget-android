package kz.q19.kenes.widget.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.card.MaterialCardView
import kz.q19.kenes.widget.R
import kz.q19.utils.android.dp2Px

internal class KenesFloatingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = KenesFloatingLayout::class.java.simpleName
    }

    class Padding constructor(
        var bottom: Float = 0F,
        var left: Float = 0F,
        var right: Float = 0F,
        var top: Float = 0F
    )

    private val touchSlop: Float = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var padding: Padding = Padding()

    init {
        padding.bottom = 16F.dp2Px()
        padding.left = 16F.dp2Px()
        padding.right = 16F.dp2Px()
        padding.top = 16F.dp2Px()
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
        return super.onInterceptTouchEvent(ev)
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

                    val maxLeft = padding.left
                    val maxRight = padding.right
                    val maxTop = padding.top
                    val maxBottom = padding.bottom

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

    fun setPadding(padding: Padding) {
        this.padding = padding
    }

}