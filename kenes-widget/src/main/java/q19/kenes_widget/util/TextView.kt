package q19.kenes_widget.util

import android.widget.TextView
import androidx.annotation.DrawableRes

fun TextView.showCompoundDrawableOnfLeft(@DrawableRes drawableRes: Int, padding: Int? = null) {
    setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0)
    if (padding != null) {
        compoundDrawablePadding = padding
    }
}

fun TextView.showCompoundDrawableOnRight(@DrawableRes drawableRes: Int, padding: Int? = null) {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableRes, 0)
    if (padding != null) {
        compoundDrawablePadding = padding
    }
}

fun TextView.removeCompoundDrawables() {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    compoundDrawablePadding = 0
}