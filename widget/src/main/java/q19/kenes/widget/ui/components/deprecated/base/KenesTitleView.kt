package q19.kenes.widget.ui.components.deprecated.base

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import kz.q19.utils.textview.removeCompoundDrawables
import kz.q19.utils.textview.showCompoundDrawableOnLeft
import q19.kenes_widget.R

internal class KenesTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): KenesTextView(context, attrs, defStyleAttr) {

    init {
        setTextAppearance(R.style.Kenes_BoldTitle)
    }

    override fun setClickable(isClickable: Boolean) {
        super.setClickable(isClickable)
        this.isFocusable = isClickable
    }

    fun showBackButtonOnLeft() {
        showCompoundDrawableOnLeft(
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
        val stateSet = arrayOf(intArrayOf(android.R.attr.state_pressed))
        val colors = intArrayOf(ContextCompat.getColor(context, R.color.kenes_gray))

        background = RippleDrawable(
            ColorStateList(stateSet, colors),
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