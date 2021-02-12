package q19.kenes.widget.util

import android.content.res.Resources
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout

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



internal fun View.addKeyboardInsetListener(callback: (visible: Boolean) -> Unit) {
    doOnLayout {
        // get init state of keyboard
        var wasKeyboardVisible = isKeyboardVisible()

        // callback as soon as the layout is set with whether the keyboard is open or not
        callback(wasKeyboardVisible)

        // whenever there is an inset change on the App, check if the keyboard is visible.
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val isKeyboardVisible = isKeyboardVisible()
            // since the observer is hit quite often, only callback when there is a change.
            if (isKeyboardVisible != wasKeyboardVisible) {
                callback(isKeyboardVisible)
                wasKeyboardVisible = isKeyboardVisible
            }

            insets
        }
    }
}


internal fun View.isKeyboardVisible(): Boolean {
    return ViewCompat.getRootWindowInsets(this)
        ?.isVisible(WindowInsetsCompat.Type.ime())
        ?:
        WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets)
            .isVisible(WindowInsetsCompat.Type.ime())
}