package q19.kenes_widget.util

internal object UrlUtil {

    private val URL_REGEX = """^(http://|https://)\w""".toRegex()

    private var HOSTNAME: String? = null

    const val STATIC_PATH = "/static/uploads/"

    fun getStaticUrl(path: String?): String? {
        if (path == null) {
            return null
        }

        val hostname = getHostname()
        return if (!hostname.isNullOrBlank()) {
            if (path.startsWith(STATIC_PATH)) {
                hostname + path
            } else {
                if (hostname.endsWith("/")) {
                    hostname.dropLast(1) + STATIC_PATH + path
                } else {
                    hostname + STATIC_PATH + path
                }
            }
        } else {
            return null
        }
    }

    fun getWidgetUrl(): String? {
        val hostname = getHostname()
        return if (!hostname.isNullOrBlank()) {
            "$hostname/admin/widget?is_mobile=true"
        } else {
            return null
        }
    }

    fun getHostname(): String? {
        val hostname = HOSTNAME
        return if (!hostname.isNullOrBlank()) {
            if (URL_REGEX.containsMatchIn(hostname)) {
                HOSTNAME
            } else {
                null
            }
        } else {
            null
        }
    }

    fun setHostname(hostname: String) {
        HOSTNAME = hostname
    }

    fun getSignallingServerUrl(): String? {
        val hostname = getHostname()
        return if (!hostname.isNullOrBlank()) {
            when {
                "kenes.vlx.kz" in hostname -> "https://kenes2.vlx.kz/user"
                "rtc.vlx.kz" in hostname -> "https://rtc.vlx.kz"
                else -> null
            }
        } else {
            null
        }
    }

    fun buildUrl(path: String): String {
        if (path.isBlank()) return ""
        val hostname = getHostname() ?: return ""
        if (path.startsWith(hostname)) return path
        return hostname.dropLastWhile { it == '/' } + path
    }

}