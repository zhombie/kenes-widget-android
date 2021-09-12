package kz.q19.kenes.widget.ui.components

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import com.google.android.material.textfield.TextInputEditText
import kz.q19.kenes.widget.R
import kotlin.math.min
import kotlin.math.roundToInt

internal class KenesTextInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = KenesTextInputEditText::class.java.simpleName
    }

    private var maxHeight: Float? = null

    fun setMaxHeight(maxHeight: Float) {
        this.maxHeight = maxHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxHeight = maxHeight?.roundToInt()
        val newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, maxHeight)
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

    /**
     * Makes a measure spec that tries greedily to use the max value.
     *
     * @param measureSpec The measure spec.
     * @param maxSize The max value for the size.
     * @return A measure spec greedily imposing the max size.
     */
    private fun makeMeasureSpec(measureSpec: Int, maxSize: Int?): Int {
//        Logger.debug(TAG, "$lineCount, $maxLines")
        if (lineCount < maxLines) return measureSpec
        if (maxSize == null) return measureSpec
        val size = MeasureSpec.getSize(measureSpec)
        return when (val mode = MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY ->
                measureSpec
            MeasureSpec.AT_MOST ->
                MeasureSpec.makeMeasureSpec(min(size, maxSize), MeasureSpec.EXACTLY)
            MeasureSpec.UNSPECIFIED ->
                MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY)
            else ->
                throw IllegalArgumentException("Unknown measure mode: $mode")
        }
    }

}