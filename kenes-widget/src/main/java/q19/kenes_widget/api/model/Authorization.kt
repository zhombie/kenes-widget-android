package q19.kenes_widget.api.model

import android.util.Log
import q19.kenes_widget.KenesWidget
import java.io.Serializable

data class Authorization constructor(
    val bearer: Bearer
) : Serializable {

    data class Bearer constructor(
        val token: String,
        val refreshToken: String? = null,
        val scope: String? = null,
        val expiresIn: Long? = null
    ) : Serializable {

        constructor(token: String) : this(
            token = token,
            refreshToken = null,
            scope = null,
            expiresIn = null
        )

        init {
            if (token.isBlank()) {
                Log.w(KenesWidget::class.java.simpleName, "Bearer token is blank or empty!")
            }
        }

    }

}