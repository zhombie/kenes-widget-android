package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import q19.kenes_widget.R

internal class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val progressBar: ProgressBar

    init {
        val view = inflate(context, R.layout.kenes_view_progress, this)

        progressBar = view.findViewById(R.id.progressBar)
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isProgressShown(): Boolean = visibility == View.VISIBLE

    fun isProgressHidden(): Boolean = visibility == View.GONE

}