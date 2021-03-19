package q19.kenes_widget.data.model

internal data class IDP constructor(
    val person: Person? = null,
    val phoneNumber: String? = null
) {

    companion object {
        const val CLIENT_ID = "kenes"
        const val CLIENT_REDIRECT_URL = "https://kenes.vlx.kz"
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