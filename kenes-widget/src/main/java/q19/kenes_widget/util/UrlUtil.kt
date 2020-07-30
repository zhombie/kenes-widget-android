package q19.kenes_widget.util

object UrlUtil {

    private val URL_REGEX = """^(http://|https://)\w""".toRegex()

    private var HOSTNAME: String? = null

    private const val STATIC_PATH = "/static/uploads/"

    fun getStaticUrl(hash: String?): String? {
        if (hash.isNullOrBlank()) {
            return null
        }

        val hostname = getHostname()
        return if (!hostname.isNullOrBlank()) {
            if (hash.startsWith(STATIC_PATH)) {
                hostname + hash
            } else {
                if (hostname.endsWith("/")) {
                    hostname.dropLastWhile { it == '/' } + STATIC_PATH + hash
                } else {
                    hostname + STATIC_PATH + hash
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

    fun getSocketUrl(): String? {
        val hostname = getHostname()
        return if (!hostname.isNullOrBlank()) {
            val domain = when {
                "kenes.vlx.kz" in hostname -> "https://kenes2.vlx.kz"
                "rtc.vlx.kz" in hostname -> "https://rtc.vlx.kz"
                "bot.nitec.kz" in hostname || "kenes.1414.kz" in hostname -> "https://kenes2.1414.kz"
                "help.post.kz" in hostname -> "https://help.post.kz"
                else -> null
            }

            if (hostname == "rtc.vlx.kz") {
                return hostname
            }

            if (!domain.isNullOrBlank()) {
                "$domain/user"
            } else {
                null
            }
        } else {
            null
        }
    }

    fun buildUrl(path: String): String? {
        if (path.isBlank()) return null
        val hostname = getHostname()
        if (hostname.isNullOrBlank()) return null
        if (path.startsWith(hostname)) return path
        if (path.startsWith('/')) return hostname.dropLastWhile { it == '/' } + path
        return hostname.dropLastWhile { it == '/' } + '/' + path
    }

    val isDebug: Boolean
        get() {
            val hostname = getHostname()
            if (hostname.isNullOrBlank()) return false
            if (hostname.contains("kenes.vlx.kz")) return true
            if (hostname.contains("help.post.kz")) return true
            return false
        }

}