package q19.kenes_widget.util.html

import android.os.Build
import android.text.Html
import android.text.Spanned

internal object HtmlCompat {

    fun fromHtml(text: String): Spanned? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(text)
        }
    }

}