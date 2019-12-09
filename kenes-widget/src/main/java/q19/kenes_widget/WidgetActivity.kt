package q19.kenes_widget

import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_widget.*

class WidgetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        setupWebView()
        loadUrl()
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(false)
        webSettings.domStorageEnabled = true
    }

    private fun loadUrl() {
        webView.loadUrl(Constants.URL)
    }

}
