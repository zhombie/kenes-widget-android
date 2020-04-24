package q19.kenes_widget

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.nurmash.lib.nurmashwidgets.customtabs.Browser

class KenesWidgetActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WidgetActivity"

        private const val PERMISSIONS_REQUEST_CODE = 0

        private const val KEY_HOSTNAME = "hostname"

        @JvmStatic
        fun newIntent(context: Context, hostname: String): Intent =
            Intent(context, KenesWidgetActivity::class.java)
                .putExtra(KEY_HOSTNAME, hostname)
    }

    private var progressBar: ProgressBar? = null
    private var kenesWebView: KenesWebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_widget)

        val hostname = intent.getStringExtra(KEY_HOSTNAME)

        if (hostname.isNullOrBlank()) {
            throwError()
        } else {
            KenesUrlUtil.HOSTNAME = hostname
        }

        bindViews()
        checkForAndAskForPermissions()
    }

    private fun bindViews() {
        progressBar = findViewById(R.id.progressBar)
        kenesWebView = findViewById(R.id.kenesWebView)
    }

    private fun unbindViews() {
        progressBar = null
        kenesWebView = null
    }

    private fun checkForAndAskForPermissions() {
        // Check if the permissions have been granted
        if (
            isPermissionGranted(Manifest.permission.CAMERA) &&
            isPermissionGranted(Manifest.permission.MODIFY_AUDIO_SETTINGS) &&
            isPermissionGranted(Manifest.permission.RECORD_AUDIO)
        ) {
            // Permissions have already been granted
            setupWebView()
        } else {
            // Permissions are missing and must be requested
            if (
                isShouldShowRequestPermissionRationale(Manifest.permission.CAMERA) &&
                isShouldShowRequestPermissionRationale(Manifest.permission.MODIFY_AUDIO_SETTINGS) &&
                isShouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
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
                            Manifest.permission.RECORD_AUDIO
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
        kenesWebView?.setCookiesEnabled(true)
        kenesWebView?.setThirdPartyCookiesEnabled(true)
        kenesWebView?.setMixedContentAllowed(true)
        kenesWebView?.clearCache(true)
        kenesWebView?.clearHistory()

        kenesWebView?.webViewClient = object : WebViewClient() {
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
                        context = this@KenesWidgetActivity,
                        url = url,
                        fallback = KenesWebViewFallback()
                    )
                    true
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
            }
        }

//        loadCameraCallUrl()

        kenesWebView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                runOnUiThread {
                    request?.grant(request.resources)
                }
            }
        }
    }

    private fun loadWidgetUrl() {
        val url = KenesUrlUtil.getUrl()
        if (url.isNullOrBlank()) {
            throwError()
        } else {
            kenesWebView?.loadUrl(url)
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(R.string.kenes_exit_widget_title)
            .setMessage(R.string.kenes_exit_widget_text)
            .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton(R.string.kenes_no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        kenesWebView?.onResume()
        loadWidgetUrl()
    }

    override fun onPause() {
        kenesWebView?.stopLoading()
        kenesWebView?.onPause()
        clearWebViewData()
        kenesWebView?.loadUrl("about:blank")
        super.onPause()
    }

    override fun onDestroy() {
        clearWebViewData()
        kenesWebView?.destroy()
        unbindViews()
        super.onDestroy()
    }

    private fun clearWebViewData() {
        kenesWebView?.clearCache(true)
        kenesWebView?.clearHistory()
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

    private fun throwError() {
        Toast.makeText(this, "Извините, но ссылка не доступна", Toast.LENGTH_SHORT).show()
        finish()
    }

}