package q19.kenes_widget.api.model

import java.io.Serializable

class User private constructor(
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val iin: String? = null,  // 701020304050
    val phoneNumber: String? = null,  // 77771234567
    val email: String? = null,  // xyz@gmail.com
    val birthDate: String? = null  // YYYY-MM-DD (1970-12-31)
) : Serializable {

    class Builder {
        private var firstName: String? = null
        private var lastName: String? = null
        private var middleName: String? = null
        private var iin: String? = null
        private var phoneNumber: String? = null
        private var email: String? = null
        private var birthDate: String? = null

        fun setFirstName(firstName: String): Builder {
            this.firstName = firstName
            return this
        }

        fun setLastName(lastName: String): Builder {
            this.lastName = lastName
            return this
        }

        fun setMiddleName(middleName: String): Builder {
            this.middleName = middleName
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

        fun setBirthDate(birthDate: String): Builder {
            this.birthDate = birthDate
            return this
        }

        fun build(): User {
            return User(
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                iin = iin,
                phoneNumber = phoneNumber,
                email = email,
                birthDate = birthDate
            )
        }
    }

}