package q19.kenes_widget.api.model

import java.io.Serializable

data class User constructor(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null
) : Serializable