package q19.kenes_widget.ui.presentation

import android.app.Dialog
import android.net.http.SslError
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import q19.kenes_widget.R
import q19.kenes_widget.data.model.IDP
import q19.kenes_widget.data.model.Language
import q19.kenes_widget.ui.components.ProgressView
import q19.kenes_widget.ui.components.WebView
import q19.kenes_widget.util.Logger

class IDPFragment : AppCompatDialogFragment(), WebView.Listener {

    companion object {
        private val TAG = IDPFragment::class.java.simpleName

        fun newInstance(hostname: String, language: String): IDPFragment {
            val fragment = IDPFragment()
            fragment.arguments = Bundle().apply {
                putString(BundleKey.HOSTNAME, hostname)
                putString(BundleKey.LANGUAGE, language)
            }
            return fragment
        }
    }

    private object BundleKey {
        const val HOSTNAME = "hostname"
        const val LANGUAGE = "language"
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private var webView: WebView? = null
    private var progressView: ProgressView? = null
    private var overlayView: LinearLayout? = null
    private var closeButton: AppCompatImageButton? = null
    private var agreeButton: AppCompatButton? = null

    private var hostname: String? = null
    private var language: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, theme)

        hostname = requireNotNull(arguments?.getString(BundleKey.HOSTNAME))
        language = arguments?.getString(BundleKey.LANGUAGE) ?: Language.DEFAULT.key
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.attributes?.windowAnimations = R.style.FullscreenDialog_Animation
        return dialog
    }

    override fun getTheme(): Int {
        return R.style.FullscreenDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.kenes_fragment_idp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webView)
        progressView = view.findViewById(R.id.progressView)
        overlayView = view.findViewById(R.id.overlayView)
        closeButton = view.findViewById(R.id.closeButton)
        agreeButton = view.findViewById(R.id.agreeButton)

        setupOverlayView()
        setupProgressView()
        setupWebView()
    }

    override fun dismiss() {
        webView?.destroy()
        super.dismiss()
        Logger.debug(TAG, "dismiss()")
        listener?.onReceivedCode(null)
    }

    private fun setupOverlayView() {
        fun hideOverlayView() {
            overlayView?.animate()
                ?.setDuration(100L)
                ?.alpha(0.0F)
                ?.translationY(-((overlayView?.height ?: view?.height)?.toFloat() ?: 0F))
                ?.withEndAction {
                    overlayView?.visibility = View.GONE
                }
                ?.start()
        }

        overlayView?.visibility = View.VISIBLE

        closeButton?.setOnClickListener { dismiss() }

        agreeButton?.setOnClickListener {
            hideOverlayView()

            val url = buildUrl()
            webView?.loadUrl(url)
        }
    }

    private fun setupProgressView() {
        progressView?.show()
    }

    private fun setupWebView() {
        webView?.init()
        webView?.setCookiesEnabled(true)
        webView?.setThirdPartyCookiesEnabled(true)
        webView?.setMixedContentAllowed(true)
        webView?.setUrlListener { url ->
            Logger.debug(TAG, "url: $url")
            val code = url.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                webView?.stopLoading()
                webView?.destroy()
                listener?.onReceivedCode(code)
            }
        }

        webView?.setListener(this)
    }

    private fun buildUrl(): String {
        var url = "$hostname/idp/oauth/authorize"

        url += "?"
        url += "response_type=code"
        url += "&"
        url += "client_id=${IDP.CLIENT_ID}"
        url += "&"
        url += "redirect_uri=${IDP.CLIENT_REDIRECT_URL}"
        url += "&"
        url += "state=xyz"
        url += "&"
        url += "scope=${IDP.CLIENT_SCOPES.joinToString(separator = " ")}"
        url += "&"
        url += "lang=$language"

        Logger.debug(TAG, "url: $url")

        return url
    }

    /**
     * [WebView.Listener] implementation
     */

    override fun onReceivedSSLError(handler: SslErrorHandler?, error: SslError?) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.kenes_attention)
            .setMessage(R.string.kenes_error_ssl)
            .setNegativeButton(R.string.kenes_cancel) { dialog, _ ->
                dialog.dismiss()
                dismiss()
            }
            .setPositiveButton(R.string.kenes_action_proceed_anyway) { dialog, _ ->
                dialog.dismiss()
                handler?.proceed()
            }
            .show()
    }

    override fun onLoadProgress(progress: Int) {
        progressView?.showTextView()
        if (progress in 0..100) {
            progressView?.setText(getString(R.string.kenes_loading, progress))
        }
        if (progress > 60) {
            progressView?.hide()
        }
    }

    fun interface Listener {
        fun onReceivedCode(code: String?)
    }

}