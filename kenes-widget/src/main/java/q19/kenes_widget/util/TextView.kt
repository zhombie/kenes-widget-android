package q19.kenes_widget.util

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.DrawableRes

internal enum class Alignment {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

internal fun TextView.showCompoundDrawableOnfLeft(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.LEFT, padding)
}

internal fun TextView.showCompoundDrawableOnTop(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.TOP, padding)
}

internal fun TextView.showCompoundDrawableOnRight(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.RIGHT, padding)
}

internal fun TextView.showCompoundDrawableOnBottom(@DrawableRes drawableRes: Int, padding: Int? = null) {
    showCompoundDrawable(drawableRes, Alignment.BOTTOM, padding)
}

internal fun TextView.showCompoundDrawable(@DrawableRes drawableRes: Int, alignment: Alignment, padding: Int? = null) {
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

internal fun TextView.removeCompoundDrawables() {
    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    compoundDrawablePadding = 0
}

internal fun TextView.showCompoundDrawableOnfLeft(drawable: Drawable?, padding: Int? = null) {
    showCompoundDrawable(drawable, Alignment.LEFT, padding)
}

internal fun TextView.showCompoundDrawable(drawable: Drawable?, alignment: Alignment, padding: Int? = null) {
    if (drawable == null) {
        return
    }
    when (alignment) {
        Alignment.LEFT -> setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        Alignment.TOP -> setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
        Alignment.RIGHT -> setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        Alignment.BOTTOM -> setCompoundDrawablesWithIntrinsicBounds(null, null, null, drawable)
    }
    if (padding != null) {
        compoundDrawablePadding = padding
    }
}

internal fun TextView.getCompoundDrawableOnTop(): Drawable? {
    return compoundDrawables[1]
}