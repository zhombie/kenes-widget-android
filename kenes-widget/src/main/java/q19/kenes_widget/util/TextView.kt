package q19.kenes_widget.util

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.DrawableRes

enum class Alignment {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

fun TextView.showCompoundDrawableOnfLeft(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.LEFT, padding)
}

fun TextView.showCompoundDrawableOnTop(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.TOP, padding)
}

fun TextView.showCompoundDrawableOnRight(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.RIGHT, padding)
}

fun TextView.showCompoundDrawableOnBottom(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.BOTTOM, padding)
}

fun TextView.showCompoundDrawable(@DrawableRes drawableRes: Int, alignment: Alignment, padding: Int? = null) {
    when (alignment) {
        Alignment.LEFT -> setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0)
        Alignment.TOP -> setCompoundDrawablesWithIntrinsicBounds(0, drawableRes, 0, 0)
        Alignment.RIGHT -> setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableRes, 0)
        Alignment.BOTTOM -> setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, drawableRes)
    }
    if (padding != null) {
        compoundDrawablePadding = padding
    }
}

fun TextView.removeCompoundDrawables() {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    compoundDrawablePadding = 0
}

fun TextView.getCompoundDrawableOnTop(): Drawable? {
    return compoundDrawables[1]
}