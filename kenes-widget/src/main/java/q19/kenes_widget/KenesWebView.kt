package q19.kenes_widget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SENDTO
import android.os.Build
import android.util.AttributeSet
import android.webkit.*
import org.nurmash.lib.nurmashwidgets.customtabs.Browser

class KenesWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        isSaveEnabled = true

        settings.builtInZoomControls = false
        settings.setSupportZoom(false)
        settings.domStorageEnabled = true
        settings.javaScriptEnabled = true

        webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url
                val url = uri.toString()
                val scheme = uri?.scheme

                val intent: Intent?

                if (scheme != null && scheme == "mailto") {
                    intent = Intent(ACTION_SENDTO, uri)
                    try {
                        getContext().startActivity(intent)
                    } catch (ignored: ActivityNotFoundException) {
                    }

                    return true
                }

                return if (uri != null) {
                    Browser.openLink(
                        context = context,
                        url = url,
                        fallback = WebViewFallback()
                    )

                    true
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }

        }

        webChromeClient = object : WebChromeClient() {
        }
    }

    fun setCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptCookie(enabled)
    }

    fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled)
        }
    }

    fun setMixedContentAllowed(allowed: Boolean) {
        if (Build.VERSION.SDK_INT >= 21) {
            settings.mixedContentMode = if (allowed) {
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
        }
    }

}