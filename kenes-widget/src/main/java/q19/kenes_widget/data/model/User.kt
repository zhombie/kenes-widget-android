package q19.kenes_widget.data.model

import java.io.Serializable

internal data class User constructor(
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val iin: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val birthDate: String? = null
) : Serializable {

    fun isEmpty(): Boolean {
        return firstName.isNullOrBlank() && phoneNumber.isNullOrBlank()
    }

}