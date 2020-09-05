package q19.kenes_widget.ui.components.base

import android.content.Context
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
import q19.kenes_widget.R
import q19.kenes_widget.util.ColorStateListBuilder

class HtmlTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private val colorStateList by lazy {
        ColorStateListBuilder()
            .addState(
                IntArray(1) { android.R.attr.state_pressed },
                ContextCompat.getColor(context, R.color.kenes_light_blue)
            )
            .addState(
                IntArray(1) { android.R.attr.state_selected },
                ContextCompat.getColor(context, R.color.kenes_light_blue)
            )
            .addState(intArrayOf(), ContextCompat.getColor(context, R.color.kenes_light_blue))
            .build()
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
        val urls = spannableStringBuilder.getSpans(
            0, spanned.length, URLSpan::class.java
        )
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