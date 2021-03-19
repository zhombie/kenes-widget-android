package q19.kenes_widget.data.network.socket

import q19.kenes_widget.core.device.DeviceInfo
import q19.kenes_widget.data.model.Authorization

internal fun q19.kenes_widget.api.model.Authorization.toDomain(): Authorization {
    return Authorization(
        bearer = Authorization.Bearer(
            token = bearer.token,
            refreshToken = bearer.refreshToken,
            scope = bearer.scope,
            expiresIn = bearer.expiresIn
        )
    )
}


internal fun DeviceInfo.from(): CallInitialization.Device {
    return CallInitialization.Device(
        os = os,
        osVersion = osVersion,
        appVersion = versionName,
        name = deviceName,
        mobileOperator = operator,
        battery = CallInitialization.Device.Battery(
            percentage = batteryPercent,
            isCharging = isPhoneCharging,
            temperature = batteryTemperature
        )
    )
}