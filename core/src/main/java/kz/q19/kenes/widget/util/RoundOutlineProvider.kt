package kz.q19.kenes.widget.util

import android.graphics.Path
import android.graphics.RectF
import android.view.View


fun getPathOfRoundedRectF(
    view: View,
    topLeftRadius: Float = 0f,
    topRightRadius: Float = 0f,
    bottomRightRadius: Float = 0f,
    bottomLeftRadius: Float = 0f
): Path {
    val tlRadius = topLeftRadius.coerceAtLeast(0f)
    val trRadius = topRightRadius.coerceAtLeast(0f)
    val brRadius = bottomRightRadius.coerceAtLeast(0f)
    val blRadius = bottomLeftRadius.coerceAtLeast(0f)

    with(Path()) {
        moveTo(view.left + tlRadius, view.top.toFloat())

        //setup top border
        lineTo(view.right - trRadius, view.top.toFloat())

        //setup top-right corner
        arcTo(
            RectF(
                view.right - trRadius * 2f,
                view.top.toFloat(),
                view.right.toFloat(),
                view.top + trRadius * 2f
            ),
            -90f,
            90f
        )

        //setup right border
        lineTo(view.right.toFloat(), view.bottom - trRadius - brRadius)

        //setup bottom-right corner
        arcTo(
            RectF(
                view.right - brRadius * 2f,
                view.bottom - brRadius * 2f,
                view.right.toFloat(),
                view.bottom.toFloat()
            ),
            0f,
            90f
        )

        //setup bottom border
        lineTo(view.left + blRadius, view.bottom.toFloat())

        //setup bottom-left corner
        arcTo(
            RectF(
                view.left.toFloat(),
                view.bottom - blRadius * 2f,
                view.left + blRadius * 2f,
                view.bottom.toFloat()
            ),
            90f,
            90f
        )

        //setup left border
        lineTo(view.left.toFloat(), view.top + tlRadius)

        //setup top-left corner
        arcTo(
            RectF(
                view.left.toFloat(),
                view.top.toFloat(),
                view.left + tlRadius * 2f,
                view.top + tlRadius * 2f
            ),
            180f,
            90f
        )

        close()

        return this
    }
}


fun getPathOfRoundedRectF(view: View, radius: Float): Path {
    return getPathOfRoundedRectF(
        view,
        topLeftRadius = radius,
        topRightRadius = radius,
        bottomLeftRadius = radius,
        bottomRightRadius = radius
    )
}


fun getPathOfQuadTopRectF(view: View, radius: Float): Path {
    return getPathOfRoundedRectF(
        view,
        topLeftRadius = radius,
        topRightRadius = radius
    )
}


fun getPathOfQuadBottomRectF(view: View, radius: Float): Path {
    return getPathOfRoundedRectF(
        view,
        bottomLeftRadius = radius,
        bottomRightRadius = radius
    )
}