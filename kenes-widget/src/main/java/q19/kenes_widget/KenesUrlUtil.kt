package q19.kenes_widget

internal object KenesUrlUtil {

    private val URL_REGEX = """^(http://|https://)\w""".toRegex()

    var HOSTNAME: String? = null

    fun getUrl(): String? {
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