package q19.kenes_widget.legacy

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import q19.kenes_widget.R
import q19.kenes_widget.util.bind

internal class KenesWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extraUrl"
    }

    private val webView by bind<WebView>(R.id.webView)

    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_webview)

        url = intent.getStringExtra(EXTRA_URL)

        initToolbar()
        initWebView()
    }

    private fun initToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "..."
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.webChromeClient = WebChromeClient()

        webView.loadUrl(url)

        webView.settings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                supportActionBar?.title = view?.title
                supportActionBar?.subtitle = url
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun openUrlInBrowser(uri: Uri) {
        finish()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.web_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_open_in_browser ->
                openUrlInBrowser(Uri.parse(url))
        }

        return super.onOptionsItemSelected(item)
    }

}