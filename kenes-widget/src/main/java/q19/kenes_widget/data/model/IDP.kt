package q19.kenes_widget.data.model

import q19.kenes_widget.util.UrlUtil

internal data class IDP constructor(
    val person: Person? = null,
    val phoneNumber: String? = null
) {

    companion object {
        const val CLIENT_ID = "kenes"
        val CLIENT_REDIRECT_URL = UrlUtil.getHostname()
        val CLIENT_SCOPES = setOf("user:basic:read", "user:phone:read")
    }

    data class Person constructor(
        val iin: String,
        val surname: String,
        val name: String,
        val patronymic: String,
        val birthDate: String
    )

    fun isEmpty(): Boolean {
        return person?.iin.isNullOrBlank() || phoneNumber.isNullOrBlank()
    }

}