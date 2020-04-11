package q19.kenes_widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.nurmash.lib.nurmashwidgets.customtabs.Browser

internal class KenesWebViewFallback : Browser.CustomTabFallback {

    /**
     * @param context The [Context] that wants to open the Uri
     * @param uri     The [Uri] to be opened by the fallback
     */
    override fun openUri(context: Context, uri: Uri) {
        val intent = Intent(context, KenesWebViewActivity::class.java)
        intent.putExtra(KenesWebViewActivity.EXTRA_URL, uri.toString())
        context.startActivity(intent)
    }

}