package q19.kenes.widget.ui.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import q19.kenes_widget.R
import q19.kenes.widget.util.ColorStateListBuilder

fun buildSimpleDrawable(context: Context): GradientDrawable {
    val drawable = GradientDrawable()
    drawable.setColor(ContextCompat.getColor(context, R.color.kenes_very_light_mostly_white_gray))
    return drawable
}

fun buildRippleDrawable(context: Context): RippleDrawable {
    val defaultColor = ContextCompat.getColor(context, R.color.kenes_very_light_mostly_white_gray)
    val content = GradientDrawable()
    content.setColor(defaultColor)
    return RippleDrawable(
        ColorStateListBuilder()
            .addState(
                IntArray(1) { android.R.attr.state_pressed },
                ContextCompat.getColor(context, q19.kenes_widget.R.color.kenes_gray)
            )
            .addState(
                intArrayOf(),
                defaultColor
            )
            .build(),
        content,
        ShapeDrawable(RectShape())
    )
}

fun setDrawableTint(
    context: Context,
    @DrawableRes drawableRes: Int,
    @ColorInt color: Int
): Drawable? {
    val drawable = ResourcesCompat.getDrawable(
        context.resources,
        drawableRes,
        context.theme
    ) ?: return null
    val drawableWrap = DrawableCompat.wrap(drawable).mutate()
    DrawableCompat.setTint(drawableWrap, color)
    return drawableWrap
}