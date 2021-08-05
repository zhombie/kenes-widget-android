package q19.kenes.widget.ui.presentation.calls.video

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

internal class VideoCallPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val deviceInfo: DeviceInfo,
    private val peerConnectionClient: PeerConnectionClient,
    private val socketRepository: SocketRepository
) : BasePresenter<VideoCallView>(), WebRTCListener, CallListener, PeerConnectionClient.Listener,
    ChatBotListener, SocketStateListener {

    companion object {
        private val TAG = VideoCallPresenter::class.java.simpleName
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        initPeerConnection()
    }

    override fun onViewResume() {
        super.onViewResume()

        initSocket()
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
        Logger.debug(TAG, "setLocalSurfaceViewRenderer()")

        peerConnectionClient.localSurfaceViewRenderer = surfaceViewRenderer
        peerConnectionClient.initLocalCameraStream()
        peerConnectionClient.addLocalStreamToPeer()
    }

    fun setRemoteSurfaceViewRenderer(surfaceViewRenderer: SurfaceViewRenderer) {
        Logger.debug(TAG, "setRemoteSurfaceViewRenderer()")

        peerConnectionClient.remoteSurfaceViewRenderer = surfaceViewRenderer
        peerConnectionClient.initRemoteCameraStream()
    }

    /**
     * [PeerConnectionClient.Listener] implementation
     */

    override fun onLocalSessionDescription(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "onLocalSessionDescription(): ${sessionDescription.type}")

        socketRepository.sendLocalSessionDescription(sessionDescription)
    }

    override fun onLocalIceCandidate(iceCandidate: IceCandidate) {
        Logger.debug(TAG, "onLocalIceCandidate(): $iceCandidate")

        socketRepository.sendLocalIceCandidate(iceCandidate)
    }

    override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
        Logger.debug(TAG, "onIceConnectionChange(): $iceConnectionState")
    }

    override fun onRenegotiationNeeded() {
        Logger.debug(TAG, "onRenegotiationNeeded()")

        peerConnectionClient.createOffer()
    }

    override fun onAddRemoteStream(mediaStream: MediaStream) {
        Logger.debug(TAG, "onAddRemoteStream(): $mediaStream")

        peerConnectionClient.addRemoteStreamToPeer(mediaStream)
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        Logger.debug(TAG, "onRemoveStream(): $mediaStream")

        peerConnectionClient.removeStream(mediaStream)
    }

    override fun onLocalVideoCapturerCreateError(e: Exception) {
        e.printStackTrace()
    }

    override fun onPeerConnectionError(errorMessage: String) {
        Logger.error(TAG, errorMessage)
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        Logger.debug(TAG, "onSocketConnect()")

        socketRepository.sendUserLanguage(Language.RUSSIAN)
    }

    override fun onSocketDisconnect() {
        Logger.debug(TAG, "onSocketDisconnect()")
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
        Logger.debug(TAG, "onMessage(): $message")
    }

    override fun onCategories(categories: List<Category>) {
    }

    /**
     * [WebRTCListener] implementation
     */

    override fun onCallAccept() {
        Logger.debug(TAG, "onCallAccept()")

        socketRepository.sendQRTCAction(QRTCAction.PREPARE)
    }

    override fun onCallRedirect() {
        Logger.debug(TAG, "onCallRedirect()")

        initPeerConnection()

        peerConnectionClient.initLocalCameraStream()
        peerConnectionClient.addLocalStreamToPeer()

        socketRepository.sendQRTCAction(QRTCAction.PREPARE)
    }

    override fun onCallRedial() {
        Logger.debug(TAG, "onCallRedial()")
    }

    override fun onCallPrepare() {
        Logger.debug(TAG, "onCallPrepare()")

        socketRepository.sendQRTCAction(QRTCAction.READY)
    }

    override fun onCallReady() {
        Logger.debug(TAG, "onCallReady()")

        peerConnectionClient.createOffer()
    }

    override fun onCallAnswer(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "onCallAnswer(): ${sessionDescription.type}")

        peerConnectionClient.setRemoteDescription(sessionDescription)
    }

    override fun onCallOffer(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "onCallOffer(): ${sessionDescription.type}")

        peerConnectionClient.setRemoteDescription(sessionDescription)
        peerConnectionClient.createAnswer()
    }

    override fun onRemoteIceCandidate(iceCandidate: IceCandidate) {
        Logger.debug(TAG, "onRemoteIceCandidate(): $iceCandidate")

        peerConnectionClient.addRemoteIceCandidate(iceCandidate)
    }

    override fun onPeerHangupCall() {
        Logger.debug(TAG, "onPeerHangupCall()")

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

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        peerConnectionClient.dispose()

        socketRepository.removeAllListeners()

        socketRepository.unregisterSocketConnectEventListener()
        socketRepository.unregisterMessageEventListener()
        socketRepository.unregisterUsersQueueEventListener()
        socketRepository.unregisterCallAgentGreetEventListener()
        socketRepository.unregisterUserCallFeedbackEventListener()
        socketRepository.unregisterSocketDisconnectEventListener()
    }

}