package q19.kenes.widget.util

internal object UrlUtil {

    private const val PROTOCOL = "https://"
    private const val SOCKET_URL_PATH = "/user"

    enum class Project(val domain: String) {
        // eGov
        EGOV_BOT_NITEC("bot.nitec.kz") {
            override val hostname: String
                get() = EGOV_KENES_1414.hostname

            override val socketUrl: String
                get() = EGOV_KENES_1414.socketUrl
        },

        EGOV_KENES_1414("kenes.1414.kz") {
            override val hostname: String
                get() = PROTOCOL + domain

            override val socketUrl: String
                get() = PROTOCOL + "kenes2.1414.kz" + SOCKET_URL_PATH
        },

        // Kazpost

        KAZPOST("help.post.kz") {
            override val hostname: String
                get() = PROTOCOL + domain

            override val socketUrl: String
                get() = hostname + SOCKET_URL_PATH
        },

        // MVD RK
        MVD_RK_Q19("mvd.q19.kz") {
            override val hostname: String
                get() = MVD_RK_GOV.domain

            override val socketUrl: String
                get() = MVD_RK_GOV.socketUrl
        },

        MVD_RK_GOV("help.mvd.gov.kz") {
            override val hostname: String
                get() = PROTOCOL + domain

            override val socketUrl: String
                get() = hostname + SOCKET_URL_PATH
        },

        // Dev
        RTC_VLX("rtc.vlx.kz") {
            override val hostname: String
                get() = PROTOCOL + domain

            override val socketUrl: String
                get() = hostname
        },

        KENES_VLX("kenes.vlx.kz") {
            override val hostname: String
                get() = PROTOCOL + domain

            override val socketUrl: String
                get() = PROTOCOL + "kenes2.vlx.kz" + SOCKET_URL_PATH
        };

        abstract val hostname: String
        abstract val socketUrl: String
    }

    private val URL_REGEX = """^(http://|https://)\w""".toRegex()

    private const val STATIC_URL_PATH = "/static/uploads/"

    private var HOSTNAME: String? = null
        set(value) {
            field = value
            PROJECT = getCurrentProject(value)
        }

    private var PROJECT: Project? = null

    private fun getCurrentProject(hostname: String?): Project? {
        if (hostname == null) return null
        return Project.values().find {
            it.hostname == hostname
        }
    }

    fun getStaticUrl(hash: String?): String? {
        if (hash.isNullOrBlank()) {
            return null
        }

        val hostname = getHostname()
        return if (!hostname.isNullOrBlank()) {
            if (hash.startsWith(STATIC_URL_PATH)) {
                hostname + hash
            } else {
                if (hostname.endsWith('/')) {
                    hostname.dropLastWhile { it == '/' } + STATIC_URL_PATH + hash
                } else {
                    hostname + STATIC_URL_PATH + hash
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
        return PROJECT?.socketUrl
    }

    @Deprecated(
        "Unsafe function",
        replaceWith = ReplaceWith(
        "UrlUtil.getSocketUrl()",
            "q19.kenes.widget.UrlUtil.getSocketUrl()"
        )
    )
    fun getSocketUrl(hostname: String?): String? {
        return if (!hostname.isNullOrBlank()) {
            val domain = when {
                "kenes.vlx.kz" in hostname -> "https://kenes2.vlx.kz"
                "rtc.vlx.kz" in hostname -> "https://rtc.vlx.kz"
                "bot.nitec.kz" in hostname || "kenes.1414.kz" in hostname -> "https://kenes2.1414.kz"
                "help.post.kz" in hostname -> "https://help.post.kz"
                "mvd.q19.kz" in hostname || "help.mvd.gov.kz" in hostname -> "https://help.mvd.gov.kz"
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
        var hostname = getHostname()
        if (hostname.isNullOrBlank()) return null
        if (path.startsWith(hostname)) return path
        hostname = hostname.dropLastWhile { it == '/' }
        if ("bot.nitec.kz" in hostname) {
            hostname = "https://kenes.1414.kz"
        }
        if ("mvd.q19.kz" in hostname) {
            hostname = "https://help.mvd.gov.kz"
        }
        if (path.startsWith('/')) return hostname + path
        return "$hostname/$path"
    }

    val isDebug: Boolean
        get() {
            val hostname = getHostname()
            if (hostname.isNullOrBlank()) return false
            if (hostname.contains("kenes.vlx.kz")) return true
//            if (hostname.contains("help.mvd.gov.kz")) return true
            return false
        }

}