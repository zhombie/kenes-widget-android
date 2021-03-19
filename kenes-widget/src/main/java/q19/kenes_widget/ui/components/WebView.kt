package q19.kenes_widget.ui.components

import android.content.Context
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.*
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import q19.kenes_widget.R

class WebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = WebView::class.java.simpleName
    }

    private var urlListener: UrlListener? = null
    private var listener: Listener? = null

    fun setUrlListener(urlListener: UrlListener) {
        this.urlListener = urlListener
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

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
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        // Enable remote debugging via chrome://inspect
        setWebContentsDebuggingEnabled(true)
    }

    fun init() {
        webChromeClient = MyWebChromeClient()
        webViewClient = MyWebViewClient()
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

    private inner class MyWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            listener?.onLoadProgress(newProgress)
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return if (request == null) {
                false
            } else {
                val url = request.url
                urlListener?.onLoadUrl(url)
                super.shouldOverrideUrlLoading(view, request)
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            AlertDialog.Builder(context)
                .setTitle(R.string.kenes_attention)
                .setMessage(R.string.kenes_error_ssl)
                .setNegativeButton(R.string.kenes_cancel) { dialog, _ ->
                    dialog.dismiss()
                    listener?.onSSLExceptionCloseRequested(true)
                }
                .setPositiveButton(R.string.kenes_action_continue) { dialog, _ ->
                    dialog.dismiss()
                    handler?.proceed()
                }
                .show()
        }
    }

    fun interface UrlListener {
        fun onLoadUrl(url: Uri)
    }

    interface Listener {
        fun onSSLExceptionCloseRequested(isUser: Boolean)
        fun onLoadProgress(progress: Int)
    }

}