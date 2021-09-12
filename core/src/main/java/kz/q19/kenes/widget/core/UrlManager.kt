package kz.q19.kenes.widget.core

internal object UrlManager {

    private const val PROTOCOL = "https://"
    private const val SOCKET_URL_PATH = "/user"
    private const val STATIC_URL_PATH = "/static/uploads/"

    private var HOSTNAME: String? = null

    val isDebug: Boolean
        get() {
            val hostname = getHostname()
            if (hostname.isBlank()) return false
            if (hostname.contains("kenes.vlx.kz")) return true
            return false
        }

    fun getDomain(): String {
        return getHostname().removePrefix(PROTOCOL)
    }

    fun getHostname(): String {
        return requireNotNull(HOSTNAME) { "Cannot be null! Set hostname before usage" }
    }

    fun setHostname(hostname: String?) {
        requireNotNull(hostname) { "hostname cannot be null!" }
        require(hostname.startsWith(PROTOCOL)) { "hostname must start with https://*" }
        HOSTNAME = hostname.dropLastWhile { it == '/' }
    }

    fun getSocketUrl(): String {
        var hostname = getHostname()
        if (hostname.contains("kenes.vlx.kz")) {
            hostname = "https://kenes2.vlx.kz"
        }
        if (hostname.contains("kenes.1414.kz")) {
            hostname = "https://kenes2.1414.kz"
        }
        return hostname + SOCKET_URL_PATH
    }

    fun buildStaticUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val hostname = getHostname()
        if (path.startsWith(hostname)) return path
        if (path.startsWith(STATIC_URL_PATH)) return hostname + path
        return hostname + STATIC_URL_PATH + path.dropWhile { it == '/' }
    }

    fun buildUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val hostname = getHostname()
        var tempPath = path
        if (tempPath.startsWith(hostname)) return tempPath
        if (tempPath.startsWith('/')) {
            tempPath = tempPath.dropWhile { it == '/' }
        }
        return "$hostname/$tempPath"
    }

}