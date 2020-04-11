package q19.kenes_widget

import android.content.Context

internal object KenesConstants {

    private const val PROTOCOL = "https://"

    fun getProjectName(context: Context): String = context.getString(R.string.PROJECT_NAME)

    fun getHostname(context: Context): String? = when (getProjectName(context)) {
        "1414" -> "${PROTOCOL}bot.nitec.kz"
        "kazpost" -> "${PROTOCOL}help.post.kz"
        "skc" -> "${PROTOCOL}help.skc.kz"
        "mon" -> "${PROTOCOL}help.edu.kz"
        else -> null
    }

    fun getUrl(context: Context): String? =
        getHostname(context)?.let { "$it/admin/widget?is_mobile=true" }

}