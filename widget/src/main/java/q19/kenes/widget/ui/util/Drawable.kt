package q19.kenes.widget.ui.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.core.content.ContextCompat
import q19.kenes_widget.R

internal fun buildRippleDrawable(context: Context): RippleDrawable {
    val defaultColor = ContextCompat.getColor(context, R.color.kenes_very_light_mostly_white_gray)
    val content = GradientDrawable()
    content.setColor(defaultColor)
    val stateSet = arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf())
    val colors = intArrayOf(ContextCompat.getColor(context, R.color.kenes_gray), defaultColor)
    return RippleDrawable(
        ColorStateList(stateSet, colors),
        content,
        ShapeDrawable(RectShape())
    )
}