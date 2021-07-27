package q19.kenes.widget.ui.presentation.calls.pending

import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.language.Language
import kz.q19.socket.model.CallInitialization
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.core.device.DeviceInfo
import q19.kenes.widget.ui.presentation.calls.Call
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class PendingCallPresenter constructor(
    private val call: Call,
    private val language: Language,
    private val deviceInfo: DeviceInfo,
    private val socketRepository: SocketRepository
) : BasePresenter<PendingCallView>() {

    init {
        val callType = when (call) {
            is Call.Text -> CallType.TEXT
            is Call.Audio -> CallType.AUDIO
            is Call.Video -> CallType.VIDEO
            else -> throw UnsupportedOperationException("Call: $call")
        }

        socketRepository.sendCallInitialization(
            CallInitialization(
                callType = callType,
                domain = UrlUtil.getHostname()?.removePrefix("https://"),
                topic = call.topic,
                device = CallInitialization.Device(
                    os = deviceInfo.os,
                    osVersion = deviceInfo.osVersion,
                    appVersion = deviceInfo.versionName,
                    name = deviceInfo.deviceName,
                    mobileOperator = deviceInfo.operator,
                    battery = CallInitialization.Device.Battery(
                        percentage = deviceInfo.batteryPercent,
                        isCharging = deviceInfo.isPhoneCharging,
                        temperature = deviceInfo.batteryTemperature
                    )
                ),
                language = Language.RUSSIAN
            )
        )
    }

    fun onCancelCall() {
        socketRepository.sendPendingCallCancellation()

        getView().navigateToHome()
    }

}