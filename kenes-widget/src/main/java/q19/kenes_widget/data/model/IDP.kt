package q19.kenes_widget.data.model

internal data class IDP constructor(
    val person: Person? = null,
    val phoneNumber: String? = null
) {

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