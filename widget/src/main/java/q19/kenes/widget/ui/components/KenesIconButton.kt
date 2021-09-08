package q19.kenes.widget.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.ShapeAppearanceModel
import q19.kenes_widget.R

internal class KenesIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        ViewCompat.setBackgroundTintList(
            this,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.kenes_bg_button_transparent_to_gray))
        )

        elevation = 0F

        insetBottom = 0
        insetTop = 0

        minWidth = 0
        minHeight = 0

        shapeAppearanceModel = ShapeAppearanceModel
            .builder(context, R.style.Kenes_Widget_ShapeAppearance_Circle, 0)
            .build()

        iconGravity = ICON_GRAVITY_TEXT_START
        iconPadding = 0

        setRippleColorResource(R.color.kenes_gray)
        
        text = null
    }

    fun setPadding(size: Int) {
        setPadding(size, size, size, size)
    }

    fun setPadding(horizontal: Int, vertical: Int) {
        setPadding(horizontal, vertical, horizontal, vertical)
    }

}