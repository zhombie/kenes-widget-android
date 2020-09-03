package q19.kenes_widget.util

import android.content.res.Resources
import android.view.View

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()


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
inline var View.isVisible: Boolean
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
