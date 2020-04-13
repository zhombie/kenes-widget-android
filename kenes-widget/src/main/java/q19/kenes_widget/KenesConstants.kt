package q19.kenes_widget

import android.content.Context
import android.util.Log

internal object KenesConstants {

    private val URL_REGEX = """^(http://|https://)\w""".toRegex()

    private fun getHostname(context: Context): String = context.getString(R.string.HOSTNAME)

    fun getUrl(context: Context): String? {
        val hostname = getHostname(context)
        return if (hostname.isBlank()) {
            null
        } else {
            Log.d("LOL", hostname + ", " + URL_REGEX.containsMatchIn(hostname))
            if (URL_REGEX.containsMatchIn(hostname)) {
                "$hostname/admin/widget?is_mobile=true"
            } else {
                null
            }
        }
    }

}