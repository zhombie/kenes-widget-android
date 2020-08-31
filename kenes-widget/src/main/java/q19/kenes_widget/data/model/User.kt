package q19.kenes_widget.data.model

data class User(
    var firstName: String? = null,
    var lastName: String? = null,
    var phoneNumber: String? = null,
    var email: String? = null
) {

    fun isEmpty(): Boolean {
        return firstName.isNullOrBlank() && phoneNumber.isNullOrBlank()
    }

}