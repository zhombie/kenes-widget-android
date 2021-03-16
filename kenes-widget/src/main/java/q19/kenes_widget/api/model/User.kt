package q19.kenes_widget.api.model

import java.io.Serializable

class User private constructor(
    val firstName: String? = null,
    val lastName: String? = null,
    val iin: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null
) : Serializable {

    class Builder {
        private var firstName: String? = null
        private var lastName: String? = null
        private var iin: String? = null
        private var phoneNumber: String? = null
        private var email: String? = null

        fun setFirstName(firstName: String): Builder {
            this.firstName = firstName
            return this
        }

        fun setLastName(lastName: String): Builder {
            this.lastName = lastName
            return this
        }

        fun setIIN(iin: String): Builder {
            this.iin = iin
            return this
        }

        fun setPhoneNumber(phoneNumber: String): Builder {
            this.phoneNumber = phoneNumber
            return this
        }

        fun setEmail(email: String): Builder {
            this.email = email
            return this
        }

        fun build(): User {
            return User(
                firstName = firstName,
                lastName = lastName,
                iin = iin,
                phoneNumber = phoneNumber,
                email = email
            )
        }
    }

}