package q19.kenes_widget.data.network.socket

import android.location.Location
import q19.kenes_widget.data.model.CallType
import q19.kenes_widget.data.model.Language
import q19.kenes_widget.data.model.User
import java.io.Serializable

internal data class CallInitialization constructor(
    val callType: CallType,
    val userId: Long? = null,
    val domain: String? = null,
    val topic: String? = null,

    val authorization: Authorization? = null,

    val device: Device? = null,
    val location: Location? = null,
    val user: User? = null,

    val language: Language
) : Serializable {

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

    data class Device constructor(
        val os: String? = null,
        val osVersion: String? = null,
        val name: String? = null,
        val appVersion: String? = null,
        val mobileOperator: String? = null,
        val battery: Battery? = null
    ) : Serializable {

        data class Battery constructor(
            val percentage: Double? = null,
            val isCharging: Boolean? = null,
            val temperature: Float? = null
        ) : Serializable

    }

}