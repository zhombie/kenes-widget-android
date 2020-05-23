package q19.kenes_widget.ui.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

internal class DynamicHeightImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
): AppCompatImageView(context, attrs, defStyleAttr) {

    private var mHeightRatio = 0.0

    var heightRatio: Double
        get() = mHeightRatio
        set(ratio) {
            if (ratio != mHeightRatio) {
                mHeightRatio = ratio
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mHeightRatio > 0.0) {
            // set the image views size
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width * mHeightRatio).toInt()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}