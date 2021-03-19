package q19.kenes_widget.data.model

import java.io.Serializable

data class Authorization constructor(
    val bearer: Bearer
) : Serializable {

    data class Bearer constructor(
        val token: String,
        val refreshToken: String? = null,
        val scope: String? = null,
        val expiresIn: Long? = null
    ) : Serializable

}