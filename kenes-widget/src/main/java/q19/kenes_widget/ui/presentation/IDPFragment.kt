package q19.kenes_widget.ui.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import q19.kenes_widget.R
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

    private var hostname: String? = null
    private var language: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, theme)

        hostname = requireNotNull(arguments?.getString(BundleKey.HOSTNAME))
        language = arguments?.getString(BundleKey.LANGUAGE) ?: Language.DEFAULT.key
    }

    override fun getTheme(): Int {
        return R.style.FullscreenDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_idp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webView)
        progressView = view.findViewById(R.id.progressView)

        progressView?.show()

        webView?.init()
        webView?.setCookiesEnabled(true)
        webView?.setThirdPartyCookiesEnabled(true)
        webView?.setMixedContentAllowed(true)
        webView?.setUrlListener { url ->
            Logger.debug(TAG, "url: $url")
            val code = url.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                webView?.stopLoading()
                listener?.onReceivedCode(code)
            }
        }

        webView?.setListener(this)

        var url = "$hostname/idp/oauth/authorize"

        url += "?"
        url += "response_type=code"
        url += "&"
        url += "client_id=kenes"
        url += "&"
        url += "redirect_uri=https://kenes.vlx.kz"
        url += "&"
        url += "state=xyz"
        url += "&"
        url += "scope=user:basic:read user:phone:read"
        url += "&"
        url += "lang=$language"

        Logger.debug(TAG, "url: $url")

        webView?.loadUrl(url)
    }

    override fun onLoadProgress(progress: Int) {
        if (progressView?.isTextViewVisibile() != true) {
            progressView?.showTextView()
        }
        progressView?.setText("Загрузка: $progress%")
        if (progress > 70) {
            progressView?.hide()
        }
    }

    fun interface Listener {
        fun onReceivedCode(code: String)
    }

}