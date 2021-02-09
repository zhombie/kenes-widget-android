package q19.kenes.widget.util

import android.content.res.Resources
import android.view.View

fun Int.dp2Px(): Float = this.toFloat().dp2Px()

fun Float.dp2Px(): Float = this * Resources.getSystem().displayMetrics.density

fun Int.px2Dp(): Float = this.toFloat().px2Dp()

fun Float.px2Dp(): Float = this / Resources.getSystem().displayMetrics.density


/**
 * Returns true when this view's visibility is [View.VISIBLE], false otherwise.
 *
 * ```
 * if (view.isVisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.VISIBLE], false to [View.GONE].
 *
 * ```
 * view.isVisible = true
 * ```
 */
internal inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
//        if (value) {
//            if (visibility == View.VISIBLE) {
//
//            } else {
//                visibility = View.VISIBLE
//            }
//        } else {
//            if (visibility == View.GONE) {
//
//            } else {
//                visibility = View.GONE
//            }
//        }
        visibility = if (value) View.VISIBLE else View.GONE
    }
