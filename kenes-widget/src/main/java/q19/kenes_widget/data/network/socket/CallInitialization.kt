package q19.kenes_widget.data.network.socket

import android.location.Location
import q19.kenes_widget.data.model.CallType
import q19.kenes_widget.data.model.Language

internal data class CallInitialization constructor(
    val callType: CallType,
    val userId: Long? = null,
    val domain: String? = null,
    val topic: String? = null,

    val location: Location? = null,

    val device: Device? = null,

    val authorization: Authorization? = null,

    val language: Language
) {

    data class Device constructor(
        val os: String? = null,
        val osVersion: String? = null,
        val name: String? = null,
        val appVersion: String? = null,
        val mobileOperator: String? = null,
        val battery: Battery? = null
    ) {

        data class Battery constructor(
            val percentage: Double? = null,
            val isCharging: Boolean? = null,
            val temperature: Float? = null
        )

    }

    data class Authorization constructor(
        val bearer: Bearer
    ) {

        data class Bearer constructor(
            val token: String,
            val refreshToken: String? = null
        )

    }

}