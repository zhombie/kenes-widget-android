package q19.kenes.widget.ui.presentation.calls.media

import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.keyboard.button.RateButton
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.CallAction
import kz.q19.domain.model.message.Message
import kz.q19.domain.model.message.QRTCAction
import kz.q19.domain.model.webrtc.IceCandidate
import kz.q19.domain.model.webrtc.IceConnectionState
import kz.q19.domain.model.webrtc.SessionDescription
import kz.q19.socket.listener.CallListener
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.listener.SocketStateListener
import kz.q19.socket.listener.WebRTCListener
import kz.q19.socket.model.CallInitialization
import kz.q19.socket.model.Category
import kz.q19.socket.repository.SocketRepository
import kz.q19.webrtc.Options
import kz.q19.webrtc.PeerConnectionClient
import kz.q19.webrtc.core.ui.SurfaceViewRenderer
import org.webrtc.MediaStream
import q19.kenes.widget.core.device.DeviceInfo
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class VideoCallPresenter constructor(
    private val database: Database,
    private val deviceInfo: DeviceInfo,
    private val peerConnectionClient: PeerConnectionClient,
    private val socketRepository: SocketRepository
) : BasePresenter<VideoCallView>(), WebRTCListener, CallListener, PeerConnectionClient.Listener,
    ChatBotListener, SocketStateListener {

    companion object {
        private val TAG = VideoCallPresenter::class.java.simpleName
    }

    init {
        initSocket()
        initPeerConnection()

        socketRepository.sendCallInitialization(
            CallInitialization(
                callType = CallType.VIDEO,
                domain = UrlUtil.getHostname()?.removePrefix("https://"),
                topic = "sos",
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

    private fun initSocket() {
        socketRepository.setSocketStateListener(this)
        socketRepository.setChatBotListener(this)
        socketRepository.setWebRTCListener(this)
        socketRepository.setCallListener(this)

        socketRepository.registerSocketConnectEventListener()
        socketRepository.registerMessageEventListener()
        socketRepository.registerUsersQueueEventListener()
        socketRepository.registerCallAgentGreetEventListener()
        socketRepository.registerUserCallFeedbackEventListener()
        socketRepository.registerSocketDisconnectEventListener()

        if (!socketRepository.isConnected()) {
            socketRepository.connect()
        }
    }

    private fun initPeerConnection() {
        val iceServers = database.getIceServers()

        peerConnectionClient.createPeerConnection(
            options = Options(
                isLocalAudioEnabled = true,
                isLocalVideoEnabled = true,
                isRemoteAudioEnabled = true,
                isRemoteVideoEnabled = true,
                iceServers = iceServers ?: emptyList()
            ),
            listener = this
        )
    }

    fun setLocalSurfaceViewRenderer(surfaceViewRenderer: SurfaceViewRenderer) {
        peerConnectionClient.localSurfaceViewRenderer = surfaceViewRenderer
        peerConnectionClient.initLocalCameraStream()
        peerConnectionClient.addLocalStreamToPeer()
    }

    fun setRemoteSurfaceViewRenderer(surfaceViewRenderer: SurfaceViewRenderer) {
        peerConnectionClient.remoteSurfaceViewRenderer = surfaceViewRenderer
        peerConnectionClient.initRemoteCameraStream()
    }

    /**
     * [PeerConnectionClient.Listener] implementation
     */

    override fun onLocalSessionDescription(sessionDescription: SessionDescription) {
        socketRepository.sendLocalSessionDescription(sessionDescription)
    }

    override fun onLocalIceCandidate(iceCandidate: IceCandidate) {
        socketRepository.sendLocalIceCandidate(iceCandidate)
    }

    override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddRemoteStream(mediaStream: MediaStream) {
        peerConnectionClient.addRemoteStreamToPeer(mediaStream)
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        peerConnectionClient.removeStream(mediaStream)
    }

    override fun onLocalVideoCapturerCreateError(e: Exception) {
    }

    override fun onPeerConnectionError(errorMessage: String) {
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        socketRepository.sendUserLanguage(Language.RUSSIAN)
    }

    override fun onSocketDisconnect() {
    }

    /**
     * [ChatBotListener] implementation
     */

    override fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean {
        return true
    }

    override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
        return true
    }

    override fun onMessage(message: Message) {
    }

    override fun onCategories(categories: List<Category>) {

    }

    /**
     * [WebRTCListener] implementation
     */

    override fun onCallAccept() {
        socketRepository.sendQRTCAction(QRTCAction.PREPARE)
    }

    override fun onCallRedirect() {
        initPeerConnection()

        peerConnectionClient.initLocalCameraStream()
        peerConnectionClient.addLocalStreamToPeer()

        socketRepository.sendQRTCAction(QRTCAction.PREPARE)
    }

    override fun onCallRedial() {
    }

    override fun onCallPrepare() {
        socketRepository.sendQRTCAction(QRTCAction.READY)
    }

    override fun onCallReady() {
        peerConnectionClient.createOffer()
    }

    override fun onCallAnswer(sessionDescription: SessionDescription) {
        peerConnectionClient.setRemoteDescription(sessionDescription)
    }

    override fun onCallOffer(sessionDescription: SessionDescription) {
        peerConnectionClient.setRemoteDescription(sessionDescription)
        peerConnectionClient.createAnswer()
    }

    override fun onRemoteIceCandidate(iceCandidate: IceCandidate) {
        peerConnectionClient.addRemoteIceCandidate(iceCandidate)
    }

    override fun onPeerHangupCall() {
        socketRepository.sendPendingCallCancellation()
        socketRepository.sendCallAction(CallAction.FINISH)
        peerConnectionClient.dispose()
    }

    /**
     * [CallListener] implementation
     */

    override fun onPendingUsersQueueCount(text: String?, count: Int) {
    }

    override fun onNoOnlineCallAgents(text: String?): Boolean {
        Logger.debug(TAG, "onNoOnlineCallAgents() -> text: $text")
        return true
    }

    override fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String) {
        Logger.debug(TAG, "onCallAgentGreet() -> fullName: $fullName, photoUrl: $photoUrl, text: $text")
    }

    override fun onCallFeedback(text: String, rateButtons: List<RateButton>?) {
        Logger.debug(TAG, "onCallFeedback() -> text: $text, rateButtons: $rateButtons")
    }

    override fun onLiveChatTimeout(text: String?, timestamp: Long): Boolean {
        Logger.debug(TAG, "onCallFeedback() -> text: $text, timestamp: $timestamp")
        return true
    }

    override fun onUserRedirected(text: String?, timestamp: Long): Boolean {
        Logger.debug(TAG, "onUserRedirected() -> text: $text, timestamp: $timestamp")
        return true
    }

    override fun onCallAgentDisconnected(text: String?, timestamp: Long): Boolean {
        Logger.debug(TAG, "onCallAgentDisconnected() -> text: $text, timestamp: $timestamp")
        return true
    }

    override fun onDestroy() {
        peerConnectionClient.dispose()
    }

}