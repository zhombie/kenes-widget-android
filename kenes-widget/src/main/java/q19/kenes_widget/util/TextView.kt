package q19.kenes_widget.util

import android.widget.TextView
import androidx.annotation.DrawableRes

fun TextView.showCompoundDrawableOnfLeft(@DrawableRes drawableRes: Int, padding: Int) {
    setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0)
    compoundDrawablePadding = padding
}

fun TextView.showCompoundDrawableOnRight(@DrawableRes drawableRes: Int, padding: Int) {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, drawableRes)
    compoundDrawablePadding = padding
}

fun TextView.removeCompoundDrawables() {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
}