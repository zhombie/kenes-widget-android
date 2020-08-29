package q19.kenes_widget.ui.components.core

import android.content.Context
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import q19.kenes_widget.R
import q19.kenes_widget.util.ColorStateListBuilder
import q19.kenes_widget.util.removeCompoundDrawables
import q19.kenes_widget.util.showCompoundDrawableOnfLeft

internal class TitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): TextView(context, attrs, defStyleAttr) {

    init {
        setTextAppearance(R.style.BoldTitle)
    }

    override fun setClickable(isClickable: Boolean) {
        super.setClickable(isClickable)
        this.isFocusable = isClickable
    }

    fun showBackButtonOnLeft() {
        showCompoundDrawableOnfLeft(
            R.drawable.kenes_ic_arrow_left_black,
            context.resources.getDimensionPixelOffset(R.dimen.kenes_title_compound_drawable_padding)
        )
    }

    fun hideBackButtonOnLeft() {
        if (!compoundDrawables.isNullOrEmpty()) {
            removeCompoundDrawables()
        }
    }

    fun setRippleBackground() {
        background = RippleDrawable(
            ColorStateListBuilder()
                .addState(
                    IntArray(1) { android.R.attr.state_pressed },
                    ContextCompat.getColor(context, R.color.kenes_grayish)
                )
                .build(),
            null,
            ShapeDrawable(RectShape())
        )
    }

    fun removeBackground() {
        background = null
    }

    fun removeClickListeners() {
        if (hasOnClickListeners()) {
            setOnClickListener(null)
        }
    }

}