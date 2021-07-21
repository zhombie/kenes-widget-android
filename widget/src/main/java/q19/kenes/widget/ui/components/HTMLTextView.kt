package q19.kenes.widget.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import q19.kenes_widget.R

class HTMLTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
    defStyleRes: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = HTMLTextView::class.java.simpleName
    }

    private val colorStateList: ColorStateList by lazy {
        ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_enabled)
            ),
            intArrayOf(
                ContextCompat.getColor(context, R.color.kenes_blue),
                ContextCompat.getColor(context, R.color.kenes_blue),
                ContextCompat.getColor(context, R.color.kenes_blue)
            )
        )
    }

    init {
        highlightColor = Color.TRANSPARENT

        setLinkTextColor(colorStateList)
    }

    fun enableAutoLinkMask() {
        autoLinkMask = Linkify.ALL
    }

    fun enableLinkMovementMethod() {
        movementMethod = LinkMovementMethod.getInstance()
    }

    fun setHtmlText(spanned: Spanned?, listener: (view: View, url: String) -> Unit) {
        if (spanned == null) return
        val spannableStringBuilder = SpannableStringBuilder(spanned)
        val urls = spannableStringBuilder.getSpans(0, spanned.length, URLSpan::class.java)
        for (span in urls) {
            spannableStringBuilder.setLinkClickable(span, listener)
        }
        text = spannableStringBuilder
    }

    private fun SpannableStringBuilder.setLinkClickable(
        urlSpan: URLSpan?,
        listener: (view: View, url: String) -> Unit
    ) {
        val start = getSpanStart(urlSpan)
        val end = getSpanEnd(urlSpan)
        val flags = getSpanFlags(urlSpan)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                listener.invoke(view, urlSpan?.url ?: return)
            }
        }
        setSpan(clickableSpan, start, end, flags)
        removeSpan(urlSpan)
    }

}