package q19.kenes.widget.ui.components.base

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

internal class DynamicHeightImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
): AppCompatImageView(context, attrs, defStyleAttr) {

    private var mHeightRatio = 0F

    var heightRatio: Float
        get() = mHeightRatio
        set(value) {
            if (value != mHeightRatio) {
                mHeightRatio = value
                requestLayout()
            }
        }

    private var listener: ((width: Int, height: Int) -> Unit)? = null

    fun setMeasureChangeListener(listener: (width: Int, height: Int) -> Unit) {
        this.listener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mHeightRatio > 0) {
            // set the image views size
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width * mHeightRatio).toInt()
            listener?.invoke(width, height)
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

}