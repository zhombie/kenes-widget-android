package q19.kenes.widget.ui.presentation.call.video

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
import q19.kenes.widget.ui.presentation.call.Call
import q19.kenes.widget.ui.presentation.call.CallInteractor
import q19.kenes.widget.ui.presentation.common.BottomSheetState
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class VideoCallPresenter constructor(
    private val language: Language,
    private val call: Call,
    private val database: Database,
    private val deviceInfo: DeviceInfo,
    private val peerConnectionClient: PeerConnectionClient,
    private val socketRepository: SocketRepository
) : BasePresenter<VideoCallView>(),
    CallInteractor.CallStateListener,
    PeerConnectionClient.Listener,
    SocketStateListener,
    ChatBotListener,
    CallListener,
    WebRTCListener {

    companion object {
        private val TAG = VideoCallPresenter::class.java.simpleName
    }

    private val interactor = CallInteractor().apply {
        listener = this@VideoCallPresenter
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        interactor.isLocalAudioEnabled = call is Call.Audio || call is Call.Video
        interactor.isLocalVideoEnabled = call is Call.Video
        interactor.isRemoteAudioEnabled = call is Call.Audio || call is Call.Video
        interactor.isRemoteVideoEnabled = call is Call.Video

        initPeerConnection(
            Options(
                isLocalAudioEnabled = interactor.isLocalAudioEnabled,
                isLocalVideoEnabled = interactor.isLocalVideoEnabled,
                isRemoteAudioEnabled = interactor.isRemoteAudioEnabled,
                isRemoteVideoEnabled = interactor.isRemoteVideoEnabled,
            )
        )

        initCall()
    }

    override fun onViewResume() {
        super.onViewResume()

        Logger.debug(TAG, "onViewResume()")

        initSocket()
    }

    private fun initSocket() {
        Logger.debug(TAG, "initSocket()")

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

    private fun initCall() {
        val callType = when (call) {
            is Call.Text -> CallType.TEXT
            is Call.Audio -> CallType.AUDIO
            is Call.Video -> CallType.VIDEO
            else -> throw UnsupportedOperationException("Call: $call")
        }

        interactor.callState = CallInteractor.CallState.Pending

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

    private fun initPeerConnection(options: Options) {
        val iceServers = database.getIceServers()

        peerConnectionClient.createPeerConnection(
            options = options.copy(iceServers = iceServers ?: emptyList()),
            listener = this
        )
    }

    fun onViewReady() {
        Logger.debug(TAG, "onViewReady()")

        Logger.debug(TAG, "setupLocalAudio()")
        if (interactor.isLocalAudioEnabled) {
            getView().setLocalAudioEnabled()
        } else {
            getView().setLocalAudioDisabled()
        }

        Logger.debug(TAG, "setupLocalVideo()")
        if (interactor.isLocalVideoEnabled) {
            getView().setLocalVideoEnabled()
        } else {
            getView().setLocalVideoDisabled()
        }
    }

    fun initLocalVideostream(surfaceViewRenderer: SurfaceViewRenderer) {
        Logger.debug(TAG, "setLocalSurfaceViewRenderer()")

        peerConnectionClient.setLocalSurfaceView(surfaceViewRenderer)
        peerConnectionClient.initLocalCameraStream(isMirrored = false, isZOrderMediaOverlay = true)
    }

    fun initRemoteVideostream(surfaceViewRenderer: SurfaceViewRenderer) {
        Logger.debug(TAG, "setRemoteSurfaceViewRenderer()")

        peerConnectionClient.setRemoteSurfaceView(surfaceViewRenderer)
        peerConnectionClient.initRemoteCameraStream(isMirrored = false, isZOrderMediaOverlay = false)
    }

    fun onMinimizeClicked() {
        getView().collapseBottomSheet()
    }

    fun onToggleLocalAudio() {
        interactor.isLocalAudioEnabled = !interactor.isLocalAudioEnabled

        if (peerConnectionClient.setLocalAudioEnabled(interactor.isLocalAudioEnabled)) {
            if (interactor.isLocalAudioEnabled) {
                getView().setLocalAudioEnabled()
            } else {
                getView().setLocalAudioDisabled()
            }
        }
    }

    fun onToggleLocalCamera() {
        interactor.isLocalVideoEnabled = !interactor.isLocalVideoEnabled

        if (peerConnectionClient.setLocalVideoEnabled(interactor.isLocalVideoEnabled)) {
            if (interactor.isLocalVideoEnabled) {
                getView().setLocalVideoEnabled()
            } else {
                getView().setLocalVideoDisabled()
            }
        }
    }

    fun onToggleLocalCameraSource() {
        peerConnectionClient.onSwitchCamera(
            onDone = {},
            onError = {
                Logger.error(TAG, "onSwitchCamera() -> error -> $it")
            }
        )
    }

    fun onShowVideoCallScreen() {
        getView().expandBottomSheet()
    }

    fun onBottomSheetStateChanged(state: BottomSheetState) {
//        Logger.debug(TAG, "onBottomSheetStateChanged() -> $state")

        interactor.bottomSheetState = state

        if (interactor.callState == CallInteractor.CallState.Live) {
            when (interactor.bottomSheetState) {
                BottomSheetState.DRAGGING, BottomSheetState.SETTLING -> {
                    setLocalVideostreamPaused()
                    setRemoteVideostreamPaused()
                }
                BottomSheetState.COLLAPSED -> {
                    getView().enterFloatingVideostream()
                }
                BottomSheetState.EXPANDED -> {
                    getView().exitFloatingVideostream()
                }
                else -> {
                }
            }
        }
    }

    fun onBackPressed(): Boolean {
        Logger.debug(TAG, "onBackPressed()")

        if (interactor.bottomSheetState == BottomSheetState.EXPANDED) {
            getView().collapseBottomSheet()
            return false
        }

        return onHangupCall()
    }

    fun onHangupCall(): Boolean {
        Logger.debug(TAG, "onHangupCall()")

        return when (interactor.callState) {
            CallInteractor.CallState.Pending -> {
                getView().showCancelPendingConfirmationMessage()
                false
            }
            CallInteractor.CallState.Live -> {
                getView().showCancelLiveCallConfirmationMessage()
                false
            }
            else -> {
                interactor.callState = CallInteractor.CallState.Disconnected.User
                true
            }
        }
    }

    fun onCancelPendingCall() {
        interactor.callState = CallInteractor.CallState.Disconnected.User

        socketRepository.sendPendingCallCancellation()
    }

    fun onCancelLiveCall() {
        interactor.callState = CallInteractor.CallState.Disconnected.User

        socketRepository.sendCallAction(action = CallAction.FINISH)
    }

    fun setLocalVideostream(surfaceViewRenderer: SurfaceViewRenderer, isZOrderMediaOverlay: Boolean) {
        peerConnectionClient.setLocalVideoSink(
            surfaceViewRenderer = surfaceViewRenderer,
            isMirrored = false,
            isZOrderMediaOverlay = isZOrderMediaOverlay
        )
    }

    fun setRemoteVideostream(surfaceViewRenderer: SurfaceViewRenderer, isZOrderMediaOverlay: Boolean) {
        peerConnectionClient.setRemoteVideoSink(
            surfaceViewRenderer = surfaceViewRenderer,
            isMirrored = false,
            isZOrderMediaOverlay = isZOrderMediaOverlay
        )
    }

    fun setLocalVideostreamPaused() {
//        Logger.debug(TAG, "setLocalVideostreamPaused()")
        peerConnectionClient.localSurfaceViewRenderer?.setFpsReduction(0F)
    }

    fun setRemoteVideostreamPaused() {
//        Logger.debug(TAG, "setRemoteVideostreamPaused()")
        peerConnectionClient.remoteSurfaceViewRenderer?.setFpsReduction(0F)
    }

    fun setLocalVideostreamResumed() {
//        Logger.debug(TAG, "setLocalVideostreamResumed()")
        peerConnectionClient.localSurfaceViewRenderer?.setFpsReduction(30F)
    }

    fun setRemoteVideostreamResumed() {
//        Logger.debug(TAG, "setRemoteVideostreamResumed()")
        peerConnectionClient.remoteSurfaceViewRenderer?.setFpsReduction(30F)
    }

    /**
     * [CallInteractor.CallStateListener] implementation
     */

    override fun onNewBottomSheetState(state: BottomSheetState) {
    }

    override fun onNewCallState(state: CallInteractor.CallState) {
        when (state) {
            is CallInteractor.CallState.IDLE -> {
                getView().hideFloatingVideostreamView()
                getView().hideVideoCallScreenSwitcher()
                getView().hideHangupCallButton()
            }
            is CallInteractor.CallState.Pending -> {
                getView().hideFloatingVideostreamView()
                getView().hideVideoCallScreenSwitcher()
                getView().showHangupCallButton()
            }
            is CallInteractor.CallState.Live -> {
                getView().showFloatingVideostreamView()
                getView().showVideoCallScreenSwitcher()
                getView().showHangupCallButton()
            }
            is CallInteractor.CallState.Disconnected -> {
                getView().hideFloatingVideostreamView()
                getView().hideVideoCallScreenSwitcher()
                getView().hideHangupCallButton()
            }
        }
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

//        peerConnectionClient.createOffer()
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

        getView().showNewChatMessage(message)
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

        interactor.callState = CallInteractor.CallState.Disconnected.CallAgent

        getView().collapseBottomSheet()

        peerConnectionClient.dispose()

        getView().showNewChatMessage(
            Message.Builder()
                .setType(Message.Type.NOTIFICATION)
                .setText("Оператор отключился")
                .build()
        )
    }

    /**
     * [CallListener] implementation
     */

    override fun onPendingUsersQueueCount(text: String?, count: Int) {
    }

    override fun onNoOnlineCallAgents(text: String?): Boolean {
        Logger.debug(TAG, "onNoOnlineCallAgents() -> text: $text")

        interactor.callState = CallInteractor.CallState.IDLE

        getView().showNoOnlineCallAgentsMessage(text)

        return true
    }

    override fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String) {
        Logger.debug(TAG, "onCallAgentGreet() -> " +
            "fullName: $fullName, photoUrl: $photoUrl, text: $text")

        interactor.callState = CallInteractor.CallState.Live

        peerConnectionClient.addLocalStreamToPeer()

        val fullUrl = UrlUtil.buildUrl(photoUrl)

        getView().expandBottomSheet()

        getView().showCallAgentInfo(fullName, fullUrl)

        getView().showNewChatMessage(
            Message.Builder()
                .setType(Message.Type.INCOMING)
                .setText(text)
                .build()
        )
    }

    override fun onCallFeedback(text: String, rateButtons: List<RateButton>?) {
        Logger.debug(TAG, "onCallFeedback() -> text: $text, rateButtons: $rateButtons")
    }

    override fun onLiveChatTimeout(text: String?, timestamp: Long): Boolean {
        Logger.debug(TAG, "onCallFeedback() -> text: $text, timestamp: $timestamp")

        interactor.callState = CallInteractor.CallState.Disconnected.Timeout

        getView().collapseBottomSheet()

        return true
    }

    override fun onUserRedirected(text: String?, timestamp: Long): Boolean {
        Logger.debug(TAG, "onUserRedirected() -> text: $text, timestamp: $timestamp")
        return true
    }

    override fun onCallAgentDisconnected(text: String?, timestamp: Long): Boolean {
        Logger.debug(TAG, "onCallAgentDisconnected() -> text: $text, timestamp: $timestamp")

        interactor.callState = CallInteractor.CallState.Disconnected.CallAgent

        getView().collapseBottomSheet()

        return true
    }

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        interactor.chatMessages.clear()
        interactor.listener = null

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