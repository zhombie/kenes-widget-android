package q19.kenes_widget.ui.util

import android.content.Context
import android.graphics.Color
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
import q19.kenes_widget.util.ColorStateListBuilder

internal fun getGrayColor(): Int {
    return Color.parseColor("#FAFAFA")
}

internal fun buildSimpleDrawable(): GradientDrawable {
    val drawable = GradientDrawable()
    drawable.setColor(getGrayColor())
    return drawable
}

internal fun buildRippleDrawable(context: Context): RippleDrawable {
    val defaultColor = getGrayColor()
    val content = GradientDrawable()
    content.setColor(defaultColor)
    return RippleDrawable(
        ColorStateListBuilder()
            .addState(
                IntArray(1) { android.R.attr.state_pressed },
                ContextCompat.getColor(context, q19.kenes_widget.R.color.kenes_grayish)
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

internal fun setDrawableTint(
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