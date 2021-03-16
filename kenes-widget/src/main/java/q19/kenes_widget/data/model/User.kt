package q19.kenes_widget.data.model

import java.io.Serializable

data class User constructor(
    var firstName: String? = null,
    var lastName: String? = null,
    var phoneNumber: String? = null,
    var email: String? = null
) : Serializable {

    fun isEmpty(): Boolean {
        return firstName.isNullOrBlank() && phoneNumber.isNullOrBlank()
    }

}