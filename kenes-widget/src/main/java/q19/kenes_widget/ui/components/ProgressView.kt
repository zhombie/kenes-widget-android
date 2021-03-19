package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import q19.kenes_widget.R

class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val progressBar: ProgressBar
    private val textView: AppCompatTextView

    init {
        val view = inflate(context, R.layout.kenes_view_progress, this)

        progressBar = view.findViewById(R.id.progressBar)
        textView = view.findViewById(R.id.textView)
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isProgressShown(): Boolean = visibility == View.VISIBLE

    fun isProgressHidden(): Boolean = visibility == View.GONE

    fun showTextView() {
        if (textView.visibility != View.VISIBLE) {
            textView.visibility = View.VISIBLE
        }
    }

    fun hideTextView() {
        if (textView.visibility != View.GONE) {
            textView.visibility = View.GONE
        }
    }

    fun isTextViewVisibile(): Boolean = visibility == View.VISIBLE

    fun isTextViewHidden(): Boolean = visibility == View.GONE

    fun setText(text: String) {
        textView.text = text
    }

}