package q19.kenes.widget.ui.presentation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

internal class KenesWebView @JvmOverloads constructor(
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

        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false

        // Enable remote debugging via chrome://inspect
        setWebContentsDebuggingEnabled(true)
    }

    fun setCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptCookie(enabled)
    }

    fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled)
    }

    fun setMixedContentAllowed(allowed: Boolean) {
        settings.mixedContentMode = if (allowed) {
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        } else {
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
    }

}