package q19.kenes_widget.util

internal object UrlUtil {

    private val URL_REGEX = """^(http://|https://)\w""".toRegex()

    var HOSTNAME: String? = null

    private const val STATIC_PATH = "/static/uploads/"

//    const val SIGNALLING_SERVER_URL = "https://kenes2.vlx.kz/user"
    const val SIGNALLING_SERVER_URL = "https://rtc.vlx.kz"

    fun getStaticUrl(path: String?): String? {
        if (path == null) {
            return null
        }

        return HOSTNAME?.let {
            if (it.isBlank()) {
                null
            } else {
                if (URL_REGEX.containsMatchIn(it)) {
                    if (path.startsWith(STATIC_PATH)) {
                        it + path
                    } else {
                        if (it.endsWith("/")) {
                            it.dropLast(1) + STATIC_PATH + path
                        } else {
                            it + STATIC_PATH + path
                        }
                    }
                } else {
                    null
                }
            }
        }
    }

    fun getWidgetUrl(): String? {
        return HOSTNAME?.let {
            if (it.isBlank()) {
                null
            } else {
                if (URL_REGEX.containsMatchIn(it)) {
                    "$it/admin/widget?is_mobile=true"
                } else {
                    null
                }
            }
        }
    }

}