package q19.kenes.widget.util

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable.withTint(@ColorInt tint: Int): Drawable {
    val drawableWrap = DrawableCompat.wrap(this).mutate()
    DrawableCompat.setTint(drawableWrap, tint)
    return drawableWrap
}