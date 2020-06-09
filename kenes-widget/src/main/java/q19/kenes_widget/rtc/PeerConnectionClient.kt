package q19.kenes_widget.rtc

import android.app.Activity
import android.media.AudioManager
import android.util.Log
import org.webrtc.*
import q19.kenes_widget.util.CodecUtil
import q19.kenes_widget.util.Logger.debug
import java.util.concurrent.Executors

internal class PeerConnectionClient {

    companion object {
        private const val TAG = "PeerConnectionClient"

        const val VIDEO_RESOLUTION_WIDTH = 1024
        const val VIDEO_RESOLUTION_HEIGHT = 768
        const val FPS = 24

        const val AUDIO_TRACK_ID = "ARDAMSa0"
        const val VIDEO_TRACK_ID = "ARDAMSv0"
    }

    private var activity: Activity? = null

    private val executor = Executors.newSingleThreadExecutor()

    private var isVideoCall: Boolean = false

    private var iceServers: List<PeerConnection.IceServer>? = null

    private var sdpMediaConstraints: MediaConstraints? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var eglBase: EglBase? = null

    private val sdpObserver = InnerSdpObserver()

    private var localSurfaceView: SurfaceViewRenderer? = null
    private var remoteSurfaceView: SurfaceViewRenderer? = null

    private var encoderFactory: VideoEncoderFactory? = null
    private var decoderFactory: VideoDecoderFactory? = null

    private var localAudioSource: AudioSource? = null
    private var localVideoSource: VideoSource? = null

    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localMediaStream: MediaStream? = null

    private var localVideoCapturer: VideoCapturer? = null

    private var localAudioTrack: AudioTrack? = null
    private var remoteAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var localSdp: SessionDescription? = null

    private var isInitiator = false

    private var audioManager: AppRTCAudioManager? = null

    private var listener: Listener? = null

    fun init(
        activity: Activity,
        isVideoCall: Boolean,
        localSurfaceView: SurfaceViewRenderer,
        remoteSurfaceView: SurfaceViewRenderer,
        iceServers: List<PeerConnection.IceServer>,
        videoCodecHwAcceleration: Boolean = true,
        listener: Listener
    ) {
        this.activity = activity
        this.isVideoCall = isVideoCall
        this.eglBase = EglBase.create()

        if (isVideoCall) {
            this.localSurfaceView = localSurfaceView
            this.remoteSurfaceView = remoteSurfaceView
        }

        this.iceServers = iceServers
        this.listener = listener

        isInitiator = false

        sdpMediaConstraints = null

        localSdp = null

        if (isVideoCall) {
            activity.runOnUiThread {
                localSurfaceView.init(eglBase?.eglBaseContext, null)
                localSurfaceView.setEnableHardwareScaler(true)
                localSurfaceView.setMirror(false)
                localSurfaceView.setZOrderMediaOverlay(true)
                localSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)

                remoteSurfaceView.init(eglBase?.eglBaseContext, null)
                remoteSurfaceView.setEnableHardwareScaler(true)
                remoteSurfaceView.setMirror(true)
                remoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            }
        }

        sdpMediaConstraints = MediaConstraints()

        sdpMediaConstraints?.mandatory?.add(
            MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        )

        if (isVideoCall) {
            sdpMediaConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
            )
        }

        executor.execute {
            val initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(activity)
                .setEnableInternalTracer(true)
                .createInitializationOptions()

            PeerConnectionFactory.initialize(initializationOptions)

            val options = PeerConnectionFactory.Options()
            options.disableNetworkMonitor = true

            if (isVideoCall) {
                if (videoCodecHwAcceleration) {
                    encoderFactory = DefaultVideoEncoderFactory(
                        eglBase?.eglBaseContext,  /* enableIntelVp8Encoder */
                        true,  /* enableH264HighProfile */
                        true
                    )

                    decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
                } else {
                    encoderFactory = SoftwareVideoEncoderFactory()
                    decoderFactory = SoftwareVideoDecoderFactory()
                }
            }

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
//                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()

            peerConnection = peerConnectionFactory?.let { createPeerConnection(it) }

            localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")

            if (isVideoCall) {
                localMediaStream?.addTrack(createVideoTrack())
            }

            localMediaStream?.addTrack(createAudioTrack())

            localMediaStream?.let { addLocalStream(it) }
        }

        activity.runOnUiThread {
            audioManager = AppRTCAudioManager.create(activity)
            audioManager?.start { selectedAudioDevice, availableAudioDevices ->
                debug(TAG, "onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice")
            }
        }

        activity.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    private fun createVideoTrack(): VideoTrack? {
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)

        localVideoSource = peerConnectionFactory?.createVideoSource(false)

        localVideoCapturer = createVideoCapturer()
        localVideoCapturer?.initialize(surfaceTextureHelper, activity, localVideoSource?.capturerObserver)
        localVideoCapturer?.startCapture(
            VIDEO_RESOLUTION_WIDTH,
            VIDEO_RESOLUTION_HEIGHT,
            FPS
        )

        localVideoTrack = peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack?.setEnabled(true)

        val videoSink = ProxyVideoSink()
        videoSink.setTarget(localSurfaceView)
        localVideoTrack?.addSink(videoSink)

        return localVideoTrack
    }

    private fun createAudioTrack(): AudioTrack? {
        localAudioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())

        localAudioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        localAudioTrack?.setEnabled(true)

        return localAudioTrack
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return if (useCamera2()) {
            createCameraCapturer(Camera2Enumerator(activity))
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // find the front facing camera and return it.
        deviceNames
            .filter { enumerator.isFrontFacing(it) }
            .mapNotNull { enumerator.createCapturer(it, null) }
            .forEach { return it }
        return null
    }

    private fun useCamera2(): Boolean = Camera2Enumerator.isSupported(activity)

    private fun addLocalStream(mediaStream: MediaStream) {
        debug(TAG, "addLocalStream: $mediaStream")

        peerConnection?.addStream(mediaStream)
    }

    fun addRemoteIceCandidate(iceCandidate: IceCandidate) {
        debug(TAG, "addIceCandidate: $iceCandidate")

        executor.execute {
            peerConnection?.addIceCandidate(iceCandidate)
        }
    }

    fun setRemoteDescription(sessionDescription: SessionDescription) {
        debug(TAG, "setRemoteDescription: $sessionDescription")

        executor.execute {
            val sdpDescription = CodecUtil.preferCodec2(
                sessionDescription.description,
                CodecUtil.AUDIO_CODEC_OPUS,
                true
            )

//            sdpDescription = Codec.setStartBitrate(Codec.AUDIO_CODEC_OPUS, false, sdpDescription, 32)

            peerConnection?.setRemoteDescription(
                sdpObserver,
                SessionDescription(sessionDescription.type, sdpDescription)
            )
        }
    }

    fun createOffer() {
        debug(TAG, "createOffer")

        executor.execute {
            isInitiator = true
            peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
        }
    }

    fun createAnswer() {
        debug(TAG, "createAnswer")

        executor.execute {
            isInitiator = false
            peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
        }
    }

    private fun createPeerConnection(factory: PeerConnectionFactory): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.RELAY

        val peerConnectionObserver = object : PeerConnection.Observer {
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                debug(TAG, "onAddTrack: $rtpReceiver")
            }

            override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
                debug(TAG, "onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                debug(TAG, "onIceConnectionChange: $iceConnectionState")

                executor.execute {
                    listener?.onIceConnectionChange(iceConnectionState)
                }
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                debug(TAG, "onIceConnectionReceivingChange: $b")
            }

            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
                debug(TAG, "onIceGatheringChange: $iceGatheringState")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                debug(TAG, "onIceCandidate: $iceCandidate")

                executor.execute {
                    listener?.onIceCandidate(iceCandidate)
                }
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                debug(TAG, "onIceCandidatesRemoved: " + iceCandidates.contentToString())
            }

            override fun onAddStream(mediaStream: MediaStream) {
                debug(TAG, "onAddStream -> audioTracks: ${mediaStream.audioTracks.size}, videoTracks: ${mediaStream.videoTracks.size}")

                if (mediaStream.audioTracks.isNotEmpty()) {
                    remoteAudioTrack = mediaStream.audioTracks[0]
                    remoteAudioTrack?.setEnabled(true)
                }

                if (isVideoCall) {
                    if (mediaStream.videoTracks.isNotEmpty()) {
                        remoteVideoTrack = mediaStream.videoTracks[0]
                        remoteVideoTrack?.setEnabled(true)

                        val remoteVideoSink = ProxyVideoSink()
                        remoteVideoSink.setTarget(remoteSurfaceView)
                        remoteVideoTrack?.addSink(remoteVideoSink)
                    }
                }
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                debug(TAG, "onRemoveStream: $mediaStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                debug(TAG, "onDataChannel: $dataChannel")
            }

            override fun onRenegotiationNeeded() {
                debug(TAG, "onRenegotiationNeeded")

                executor.execute {
                    listener?.onRenegotiationNeeded()
                }
            }
        }
        return factory.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    fun onSwitchCamera() {
        executor.execute {
            localVideoCapturer?.let { videoCapturer ->
                if (videoCapturer is CameraVideoCapturer) {
                    videoCapturer.switchCamera(null)
                }
            }
        }
    }

    fun dispose() {
        activity?.runOnUiThread {
            audioManager?.stop()
            audioManager = null

            debug(TAG, "Stopping capture.")
            try {
                localVideoCapturer?.stopCapture()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        executor.execute {
            activity?.volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

            isInitiator = false

            sdpMediaConstraints = null

            localSdp = null

            peerConnection?.dispose()

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            localVideoCapturer?.dispose()
            localVideoCapturer = null

            localVideoSource?.dispose()
            localVideoSource = null

            localAudioSource?.dispose()
            localAudioSource = null

    //        localMediaStream?.dispose()
            localMediaStream = null

            debug(TAG, "Closing peer connection factory.")
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null

            debug(TAG, "Closing peer connection done.")

//            PeerConnectionFactory.stopInternalTracingCapture()
//            PeerConnectionFactory.shutdownInternalTracer()

            eglBase?.release()
            eglBase = null

//            localVideoTrack?.dispose()
            localVideoTrack = null
//            remoteVideoTrack?.dispose()
            remoteVideoTrack = null

//            localAudioTrack?.dispose()
            localAudioTrack = null

//            remoteAudioTrack?.dispose()
            remoteAudioTrack = null

            peerConnection = null
        }
    }

    private fun reportError(errorMessage: String) {
        Log.e(TAG, "PeerConnection error: $errorMessage")
        executor.execute {
            listener?.onPeerConnectionError(errorMessage)
        }
    }

    private inner class InnerSdpObserver : SdpObserver {

        override fun onCreateSuccess(sessionDescription: SessionDescription?) {
            debug(TAG, "onCreateSuccess: $sessionDescription")

            if (sessionDescription == null) return

            if (localSdp != null) {
                reportError("Multiple SDP create.")
                return
            }

            val sdpDescription = CodecUtil.preferCodec2(
                sessionDescription.description,
                CodecUtil.AUDIO_CODEC_OPUS,
                true
            )

            localSdp = SessionDescription(sessionDescription.type, sdpDescription)

            executor.execute {
                debug(TAG, "Set local SDP from " + localSdp?.type)
                peerConnection?.setLocalDescription(sdpObserver, localSdp)
            }
        }

        override fun onSetSuccess() {
            debug(TAG, "onSetSuccess")

            executor.execute {
                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (peerConnection?.remoteDescription == null) {
                        // We've just set our local SDP so time to send it.
                        debug(TAG, "Local SDP set succesfully")
                        localSdp?.let { listener?.onLocalDescription(it) }
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        debug(TAG, "Remote SDP set succesfully")
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (peerConnection?.localDescription != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        debug(TAG, "Local SDP set succesfully")
                        localSdp?.let { listener?.onLocalDescription(it) }
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        debug(TAG, "Remote SDP set succesfully")
                    }
                }
            }
        }

        override fun onCreateFailure(error: String?) {
            debug(TAG, "onCreateFailure: $error")

            reportError("Create SDP error: $error")
        }

        override fun onSetFailure(error: String?) {
            debug(TAG, "onSetFailure: $error")

            reportError("Set SDP error: $error")
        }
    }

    interface Listener {
        fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState)
        fun onIceCandidate(iceCandidate: IceCandidate)
        fun onRenegotiationNeeded()

        fun onLocalDescription(sessionDescription: SessionDescription)

        fun onPeerConnectionError(errorMessage: String)
    }

}
