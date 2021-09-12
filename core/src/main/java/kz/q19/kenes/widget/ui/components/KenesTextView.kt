package kz.q19.kenes.widget.ui.components

import android.content.Context
import android.content.res.ColorStateList
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
import kz.q19.kenes.widget.R

internal open class KenesTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
    defStyleRes: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = KenesTextView::class.java.simpleName
    }

    init {
        val lightBlueColor = ContextCompat.getColor(context, R.color.kenes_blue_with_opacity_30)
        val blueColor = ContextCompat.getColor(context, R.color.kenes_blue)

        highlightColor = lightBlueColor

        setLinkTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_pressed),
                    intArrayOf(android.R.attr.state_selected),
                    intArrayOf(android.R.attr.state_enabled)
                ),
                intArrayOf(
                    blueColor,
                    blueColor,
                    blueColor
                )
            )
        )
    }

    fun enableAutoLinkMask() {
        autoLinkMask = Linkify.ALL
    }

    fun enableLinkMovementMethod() {
        movementMethod = LinkMovementMethod.getInstance()
    }

    fun setHtmlText(spanned: Spanned?, listener: (view: View, url: String) -> Unit) {
        text = if (spanned == null) {
            spanned
        } else {
            SpannableStringBuilder(spanned).apply {
                getSpans(0, spanned.length, URLSpan::class.java).forEach { urlSpan ->
                    if (!urlSpan.url.isNullOrBlank()) {
                        setLinkClickable(urlSpan, listener)
                    }
                }
            }
        }
    }

    private fun SpannableStringBuilder.setLinkClickable(
        urlSpan: URLSpan?,
        listener: (view: View, url: String) -> Unit
    ) {
        if (urlSpan == null) return
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                listener.invoke(view, urlSpan.url ?: return)
            }
        }
        setSpan(clickableSpan, getSpanStart(urlSpan), getSpanEnd(urlSpan), getSpanFlags(urlSpan))
        removeSpan(urlSpan)
    }

}