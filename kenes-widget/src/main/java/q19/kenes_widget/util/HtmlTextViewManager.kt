package q19.kenes_widget.util

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView

class HtmlTextViewManager {

    var callback: Callback? = null

    private fun SpannableStringBuilder.setLinkClickable(urlSpan: URLSpan?) {
        val start = getSpanStart(urlSpan)
        val end = getSpanEnd(urlSpan)
        val flags = getSpanFlags(urlSpan)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                callback?.onUrlClicked(view, urlSpan?.url ?: return)
            }
        }
        setSpan(clickableSpan, start, end, flags)
        removeSpan(urlSpan)
    }

    fun setHtmlText(textView: TextView?, spanned: Spanned?) {
        if (textView == null || spanned == null) {
            return
        }

        val spannableStringBuilder = SpannableStringBuilder(spanned)
        val urls = spannableStringBuilder.getSpans(
            0, spanned.length, URLSpan::class.java
        )
        for (span in urls) {
            spannableStringBuilder.setLinkClickable(span)
        }
        textView.text = spannableStringBuilder
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    interface Callback {
        fun onUrlClicked(view: View, url: String)
    }

}