package q19.kenes_widget

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.nurmash.lib.nurmashwidgets.customtabs.Browser

class WidgetActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WidgetActivity"

        private const val PERMISSIONS_REQUEST_CODE = 0
    }

    private var progressBar: ProgressBar? = null
    private var webView: KenesWebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        bindViews()

        checkForAndAskForPermissions()
    }

    private fun bindViews() {
        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)
    }

    private fun checkForAndAskForPermissions() {
        // Check if the permissions have been granted
        if (
            isPermissionGranted(Manifest.permission.CAMERA) &&
            isPermissionGranted(Manifest.permission.MODIFY_AUDIO_SETTINGS) &&
            isPermissionGranted(Manifest.permission.RECORD_AUDIO) &&
            isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            // Permissions have already been granted
            setupWebView()
        } else {
            // Permissions are missing and must be requested
            if (
                isShouldShowRequestPermissionRationale(Manifest.permission.CAMERA) &&
                isShouldShowRequestPermissionRationale(Manifest.permission.MODIFY_AUDIO_SETTINGS) &&
                isShouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) &&
                isShouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                // Provide an additional rationale to the user if the permissions were not granted
                // and the user would benefit from additional context for the use of the permissions.
                // Display a AlertDialog to request the missing permissions.
                AlertDialog.Builder(this)
                    .setMessage("Пожалуйста, предоставьте разрешения для использования приложения")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        requestPermissions(arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ))
                    }
                    .setNegativeButton("Нет") { dialog, _ ->
                        dialog.cancel()
                        finish()
                    }
                    .show()
            } else {
                // Request the permission. The result will be received in onRequestPermissionsResult().
                requestPermissions(arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
            }
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
    }

    private fun isShouldShowRequestPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    private fun setupWebView() {
        webView?.setCookiesEnabled(true)
        webView?.setThirdPartyCookiesEnabled(true)
        webView?.setMixedContentAllowed(true)
        webView?.clearCache(true)
        webView?.clearHistory()

        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url
                val url = uri.toString()
                val scheme = uri?.scheme

                val intent: Intent?

                if (scheme != null && scheme == "mailto") {
                    intent = Intent(Intent.ACTION_SENDTO, uri)
                    try {
                        startActivity(intent)
                    } catch (ignored: ActivityNotFoundException) {
                    }

                    return true
                }

                return if (uri != null) {
                    Browser.openLink(
                        context = this@WidgetActivity,
                        url = url,
                        fallback = WebViewFallback()
                    )
                    true
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
            }
        }

        webView?.loadUrl(Constants.URL)

        webView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                runOnUiThread {
                    request?.grant(request.resources)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (webView != null && webView!!.canGoBack()) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        webView?.clearCache(true)
        webView?.clearHistory()
        webView?.destroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission has been granted
                    setupWebView()
                } else {
                    // Permission request was denied.
                    finish()
                }
            }
            else ->
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

}