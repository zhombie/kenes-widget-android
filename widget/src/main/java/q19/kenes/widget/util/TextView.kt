package q19.kenes.widget.util

import android.graphics.drawable.Drawable
import android.widget.TextView


internal fun TextView.getCompoundDrawableOnTop(): Drawable? {
    return compoundDrawables[1]
}