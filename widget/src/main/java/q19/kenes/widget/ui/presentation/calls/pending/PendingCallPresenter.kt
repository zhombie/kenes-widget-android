package q19.kenes.widget.ui.presentation.calls.pending

import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.QRTCAction
import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.SessionDescription
import kz.q19.socket.listener.CallListener
import kz.q19.socket.listener.WebRTCListener
import kz.q19.socket.model.CallInitialization
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.core.device.DeviceInfo
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.presentation.calls.Call
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class PendingCallPresenter constructor(
    private val call: Call,
    private val language: Language,
    private val deviceInfo: DeviceInfo,
    private val socketRepository: SocketRepository
) : BasePresenter<PendingCallView>(), CallListener, WebRTCListener {

    companion object {
        private val TAG = PendingCallPresenter::class.java.simpleName
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        initCall()
    }

    override fun onViewResume() {
        super.onViewResume()

        initSocket()
    }

    private fun initSocket() {
        socketRepository.setCallListener(this)
        socketRepository.setWebRTCListener(this)

        socketRepository.registerMessageEventListener()
    }

    private fun initCall() {
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
                topic = "zhombie",  // TODO: Change to 'call.topic' on production
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

    /**
     * [CallListener] implementation
     */

    override fun onPendingUsersQueueCount(text: String?, count: Int) {
    }

    override fun onNoOnlineCallAgents(text: String?): Boolean {
        getView().showNoOnlineCallAgentsMessage(text)
        return true
    }

    override fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String) {
    }

    override fun onCallFeedback(text: String, rateButtons: List<RateButton>?) {
    }

    override fun onLiveChatTimeout(text: String?, timestamp: Long): Boolean {
        return true
    }

    override fun onUserRedirected(text: String?, timestamp: Long): Boolean {
        return true
    }

    override fun onCallAgentDisconnected(text: String?, timestamp: Long): Boolean {
        return true
    }

    /**
     * [WebRTCListener] implementation
     */

    override fun onCallAccept() {
        Logger.debug(TAG, "onCallAccept()")

        socketRepository.sendQRTCAction(action = QRTCAction.PREPARE)
    }

    override fun onCallRedirect() {
        Logger.debug(TAG, "onCallRedirect()")
    }

    override fun onCallRedial() {
        Logger.debug(TAG, "onCallRedial()")
    }

    override fun onCallPrepare() {
        Logger.debug(TAG, "onCallPrepare()")

        socketRepository.sendQRTCAction(action = QRTCAction.READY)
    }

    override fun onCallReady() {
        Logger.debug(TAG, "onCallReady()")

        getView().navigateToCall(call)
    }

    override fun onCallAnswer(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "onCallAnswer()")
    }

    override fun onCallOffer(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "onCallOffer()")
    }

    override fun onRemoteIceCandidate(iceCandidate: IceCandidate) {
        Logger.debug(TAG, "onRemoteIceCandidate()")
    }

    override fun onPeerHangupCall() {
        Logger.debug(TAG, "onPeerHangupCall()")
    }

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        socketRepository.setCallListener(null)
        socketRepository.setWebRTCListener(null)

        socketRepository.unregisterMessageEventListener()
    }

}