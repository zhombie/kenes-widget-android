package q19.kenes_widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.*

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