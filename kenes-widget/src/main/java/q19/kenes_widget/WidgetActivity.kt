package q19.kenes_widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_widget.*

class WidgetActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WidgetActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        setupWebView()
    }

    private fun setupWebView() {
        webView.setCookiesEnabled(true)
        webView.setThirdPartyCookiesEnabled(true)
        webView.setMixedContentAllowed(true)
        webView.loadUrl(Constants.URL)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

}
