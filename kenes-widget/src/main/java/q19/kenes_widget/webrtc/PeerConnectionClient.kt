package q19.kenes_widget.webrtc

import android.content.Context
import android.os.Environment
import android.util.Log
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.*
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern

internal class PeerConnectionClient(
    appContext: Context,
    eglBase: EglBase,
    peerConnectionParams: PeerConnectionParams,
    events: PeerConnectionEvents
) {

    companion object {
        const val TAG = "PeerConnectionClient"

        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
        const val VIDEO_TRACK_TYPE = "video"

        private const val VIDEO_CODEC_VP8 = "VP8"
        private const val VIDEO_CODEC_VP9 = "VP9"
        private const val VIDEO_CODEC_H264 = "H264"
        private const val VIDEO_CODEC_H264_BASELINE = "H264 Baseline"
        private const val VIDEO_CODEC_H264_HIGH = "H264 High"

        private const val VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
        private const val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL =
            "WebRTC-IntelVP8/Enabled/"
        private const val DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"

        private const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
        private const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        private const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        private const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"

        private const val VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate"
        private const val AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate"

        private const val AUDIO_CODEC_OPUS = "opus"
        private const val AUDIO_CODEC_ISAC = "ISAC"

        private const val HD_VIDEO_WIDTH = 1280
        private const val HD_VIDEO_HEIGHT = 720

        private const val BPS_IN_KBPS = 1000

    }

    private var appContext: Context? = null
    private var rootEglBase: EglBase? = null
    private var peerConnectionParams: PeerConnectionParams? = null
    private var events: PeerConnectionEvents? = null

    private val executor = Executors.newSingleThreadExecutor()

    init {
        this.rootEglBase = eglBase
        this.appContext = appContext
        this.events = events
        this.peerConnectionParams = peerConnectionParams

        Log.d(TAG, "Preferred video codec: " + getSdpVideoCodecName(peerConnectionParams))

        val fieldTrials = getFieldTrials(peerConnectionParams)
        executor.execute {
            Log.d(TAG, "Initialize WebRTC. Field trials: $fieldTrials")
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                    .setFieldTrials(fieldTrials)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
        }
    }

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var localRender: VideoSink? = null
    private var remoteSinks: List<VideoSink>? = null
    private var videoCapturer: VideoCapturer? = null

    private val pcObserver = PCObserver()
    private val sdpObserver = SDPObserver()

    private var signalingParams: SignalingParams? = null

    private var videoWidth = 0
    private var videoHeight = 0
    private var videoFps = 0
    private var audioConstraints: MediaConstraints? = null
    private var sdpMediaConstraints: MediaConstraints? = null

    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var localVideoSender: RtpSender? = null

    private var localAudioTrack: AudioTrack? = null

    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private var queuedRemoteCandidates: MutableList<IceCandidate>? = null

    private var isInitiator: Boolean = false

    private var localSdp: SessionDescription? = null  // either offer or answer SDP

    private var isError = false

    private var preferIsac = false

    // enableAudio is set to true if audio should be sent.
    private var enableAudio = true

    // enableVideo is set to true if video should be rendered and sent.
    private var renderVideo = true

    private var videoCapturerStopped = false

    /**
     * This function should only be called once.
     */
    fun createPeerConnectionFactory(options: PeerConnectionFactory.Options?) {
        check(factory == null) { "PeerConnectionFactory has already been constructed" }
        executor.execute { 
            createPeerConnectionFactoryInternal(options) 
        }
    }

    fun createPeerConnection(
        localRender: VideoSink,
        remoteSink: VideoSink,
        videoCapturer: VideoCapturer?,
        signalingParams: SignalingParams
    ) {
        if (peerConnectionParams?.videoCallEnabled == true && videoCapturer == null) {
            Log.w(TAG, "Video call enabled but no video capturer provided.")
        }
        createPeerConnection(localRender, listOf(remoteSink), videoCapturer!!, signalingParams)
    }

    fun createPeerConnection(
        localRender: VideoSink,
        remoteSinks: List<VideoSink>,
        videoCapturer: VideoCapturer,
        signalingParams: SignalingParams
    ) {
        if (peerConnectionParams == null) {
            Log.e(
                TAG,
                "Creating peer connection without initializing factory."
            )
            return
        }
        this.localRender = localRender
        this.remoteSinks = remoteSinks
        this.videoCapturer = videoCapturer
        this.signalingParams = signalingParams
        executor.execute {
            try {
                createMediaConstraintsInternal()
                createPeerConnectionInternal()
            } catch (e: Exception) {
                reportError("Failed to create peer connection: " + e.message)
                throw e
            }
        }
    }

    private fun isVideoCallEnabled(): Boolean {
        return peerConnectionParams?.videoCallEnabled == true && videoCapturer != null
    }

    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options?) {
        isError = false
        if (peerConnectionParams?.tracing == true) {
            PeerConnectionFactory.startInternalTracingCapture(Environment.getExternalStorageDirectory().absolutePath + File.separator + "webrtc-trace.txt")
        }

        // Check if ISAC is used by default.
        preferIsac = (peerConnectionParams?.audioCodec != null
                && peerConnectionParams?.audioCodec == AUDIO_CODEC_ISAC)

        val adm = createJavaAudioDevice()

        // Create peer connection factory.
        if (options != null) {
            Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask)
        }
        val enableH264HighProfile = VIDEO_CODEC_H264_HIGH == peerConnectionParams?.videoCodec ?: false
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        if (peerConnectionParams?.videoCodecHwAcceleration == true) {
            encoderFactory = DefaultVideoEncoderFactory(
                rootEglBase?.eglBaseContext,
                true /* enableIntelVp8Encoder */,
                enableH264HighProfile
            )
            decoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        Log.d(TAG, "Peer connection factory created.")
        adm.release()
    }

    fun createJavaAudioDevice(): AudioDeviceModule {
        // Enable/disable OpenSL ES playback.
        if (peerConnectionParams?.useOpenSLES == false) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.")
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: AudioRecordStartErrorCode,
                errorMessage: String
            ) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
                reportError(errorMessage)
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: AudioTrackStartErrorCode,
                errorMessage: String
            ) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
                reportError(errorMessage)
            }
        }

        // Set audio record state callbacks.
        val audioRecordStateCallback: AudioRecordStateCallback = object : AudioRecordStateCallback {
            override fun onWebRtcAudioRecordStart() {
                Log.i(TAG, "Audio recording starts")
            }

            override fun onWebRtcAudioRecordStop() {
                Log.i(TAG, "Audio recording stops")
            }
        }

        // Set audio track state callbacks.
        val audioTrackStateCallback: AudioTrackStateCallback = object : AudioTrackStateCallback {
            override fun onWebRtcAudioTrackStart() {
                Log.i(TAG, "Audio playout starts")
            }

            override fun onWebRtcAudioTrackStop() {
                Log.i(TAG, "Audio playout stops")
            }
        }
        
        return builder(appContext)
            .setUseHardwareAcousticEchoCanceler(!(peerConnectionParams?.disableBuiltInAEC ?: false))
            .setUseHardwareNoiseSuppressor(!(peerConnectionParams?.disableBuiltInNS ?: false))
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecordStateCallback(audioRecordStateCallback)
            .setAudioTrackStateCallback(audioTrackStateCallback)
            .createAudioDeviceModule()
    }

    private fun createMediaConstraintsInternal() {
        // Create video constraints if video call is enabled.
        if (isVideoCallEnabled()) {
            videoWidth = peerConnectionParams?.videoWidth ?: 0
            videoHeight = peerConnectionParams?.videoHeight ?: 0
            videoFps = peerConnectionParams?.videoFps ?: 0

            // If video resolution is not specified, default to HD.
            if (videoWidth == 0 || videoHeight == 0) {
                videoWidth = PeerConnectionClient.HD_VIDEO_WIDTH
                videoHeight = PeerConnectionClient.HD_VIDEO_HEIGHT
            }

            // If fps is not specified, default to 30.
            if (videoFps == 0) {
                videoFps = 30
            }
            Logging.d(
                TAG,
                "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps
            )
        }

        // Create audio constraints.
        audioConstraints = MediaConstraints()
        // added for audio performance measurements
        if (peerConnectionParams?.noAudioProcessing == true) {
            Log.d(TAG, "Disabling audio processing")
            audioConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false")
            )
            audioConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false")
            )
            audioConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false")
            )
            audioConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false")
            )
        }
        // Create SDP constraints.
        sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints?.mandatory?.add(
            MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        )
        sdpMediaConstraints?.mandatory?.add(
            MediaConstraints.KeyValuePair("OfferToReceiveVideo", java.lang.Boolean.toString(isVideoCallEnabled()))
        )
    }

    private fun createPeerConnectionInternal() {
        if (factory == null || isError) {
            Log.e(TAG, "Peerconnection factory is not created")
            return
        }
        Log.d(TAG, "Create peer connection.")
        queuedRemoteCandidates = ArrayList<IceCandidate>()
        val rtcConfig = RTCConfiguration(signalingParams?.iceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        rtcConfig.keyType = KeyType.ECDSA
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = peerConnectionParams?.loopback == false
        rtcConfig.sdpSemantics = SdpSemantics.UNIFIED_PLAN
        peerConnection = factory?.createPeerConnection(rtcConfig, pcObserver)
        isInitiator = false

        // Set INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
        val mediaStreamLabels = listOf("ARDAMS")
        if (isVideoCallEnabled()) {
            peerConnection?.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels)
            // We can add the renderers right away because we don't need to wait for an
            // answer to get the remote track.
            remoteVideoTrack = getRemoteVideoTrack()
            remoteVideoTrack?.setEnabled(renderVideo)
            for (remoteSink in remoteSinks ?: emptyList()) {
                remoteVideoTrack?.addSink(remoteSink)
            }
        }
        peerConnection?.addTrack(createAudioTrack(), mediaStreamLabels)
        if (isVideoCallEnabled()) {
            findVideoSender()
        }
        Log.d(TAG, "Peer connection created.")
    }

    private fun closeInternal() {
        if (peerConnectionParams?.aecDump == true) {
            factory?.stopAecDump()
        }

        Log.d(TAG, "Closing peer connection.")
        peerConnection?.dispose()
        peerConnection = null

        Log.d(TAG, "Closing audio source.")
        audioSource?.dispose()
        audioSource = null

        Log.d(TAG, "Stopping capture.")
        if (videoCapturer != null) {
            try {
                videoCapturer?.stopCapture()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            videoCapturerStopped = true
            videoCapturer?.dispose()
            videoCapturer = null
        }
        Log.d(TAG, "Closing video source.")
        videoSource?.dispose()
        videoSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localRender = null
        remoteSinks = null

        Log.d(TAG, "Closing peer connection factory.")
        factory?.dispose()
        factory = null

        rootEglBase?.release()
        Log.d(TAG, "Closing peer connection done.")
        events?.onPeerConnectionClosed()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    fun isHDVideo(): Boolean {
        return isVideoCallEnabled() && videoWidth * videoHeight >= 1280 * 720
    }
    
    private fun getSdpVideoCodecName(params: PeerConnectionParams?): String {
        return when (params?.videoCodec) {
            VIDEO_CODEC_VP8 -> VIDEO_CODEC_VP8
            VIDEO_CODEC_VP9 -> VIDEO_CODEC_VP9
            VIDEO_CODEC_H264_HIGH, VIDEO_CODEC_H264_BASELINE -> VIDEO_CODEC_H264
            else -> VIDEO_CODEC_VP8
        }
    }

    private fun getFieldTrials(params: PeerConnectionParams): String {
        var fieldTrials = ""
        if (params.videoFlexfecEnabled) {
            fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL
            Log.d(TAG, "Enable FlexFEC field trial.")
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
        if (params.disableWebRtcAGCAndHPF) {
            fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL
            Log.d(TAG, "Disable WebRTC AGC field trial.")
        }
        return fieldTrials
    }

    fun setAudioEnabled(enable: Boolean) {
        executor.execute {
            enableAudio = enable
            localAudioTrack?.setEnabled(enableAudio)
        }
    }

    fun setVideoEnabled(enable: Boolean) {
        executor.execute {
            renderVideo = enable
            localVideoTrack?.setEnabled(renderVideo)
            remoteVideoTrack?.setEnabled(renderVideo)
        }
    }

    fun createOffer() {
        executor.execute {
            if (peerConnection != null && !isError) {
                Log.d(TAG, "PC Create OFFER")
                isInitiator = true
                peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
            }
        }
    }

    fun createAnswer() {
        executor.execute {
            if (peerConnection != null && !isError) {
                Log.d(TAG, "PC create ANSWER")
                isInitiator = false
                peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
            }
        }
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        executor.execute {
            if (peerConnection != null && !isError) {
                if (queuedRemoteCandidates != null) {
                    queuedRemoteCandidates?.add(candidate)
                } else {
                    peerConnection?.addIceCandidate(candidate)
                }
            }
        }
    }

    fun removeRemoteIceCandidates(candidates: Array<IceCandidate?>?) {
        executor.execute {
            if (peerConnection == null || isError) {
                return@execute
            }
            // Drain the queued remote candidates if there is any so that
            // they are processed in the proper order.
            drainCandidates()
            peerConnection?.removeIceCandidates(candidates)
        }
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        executor.execute {
            if (peerConnection == null || isError) {
                return@execute
            }
            var sdpDescription = sdp.description
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true)
            }
            if (isVideoCallEnabled()) {
                sdpDescription = preferCodec(sdpDescription, getSdpVideoCodecName(peerConnectionParams), false)
            }
            if ((peerConnectionParams?.audioStartBitrate ?: 0) > 0) {
                sdpDescription = setStartBitrate(
                    AUDIO_CODEC_OPUS,
                    false,
                    sdpDescription,
                    peerConnectionParams?.audioStartBitrate ?: 0
                )
            }
            Log.d(TAG, "Set remote SDP.")
            val sdpRemote = SessionDescription(sdp.type, sdpDescription)
            peerConnection?.setRemoteDescription(sdpObserver, sdpRemote)
        }
    }

    fun stopVideoSource() {
        executor.execute {
            if (videoCapturer != null && !videoCapturerStopped) {
                Log.d(TAG, "Stop video source.")
                try {
                    videoCapturer?.stopCapture()
                } catch (e: InterruptedException) {
                }
                videoCapturerStopped = true
            }
        }
    }

    fun startVideoSource() {
        executor.execute {
            if (videoCapturer != null && videoCapturerStopped) {
                Log.d(TAG, "Restart video source.")
                videoCapturer?.startCapture(videoWidth, videoHeight, videoFps)
                videoCapturerStopped = false
            }
        }
    }

    fun setVideoMaxBitrate(maxBitrateKbps: Int) {
        executor.execute {
            if (peerConnection == null || localVideoSender == null || isError) {
                return@execute
            }
            Log.d(TAG, "Requested max video bitrate: $maxBitrateKbps")
            if (localVideoSender == null) {
                Log.w(TAG, "Sender is not ready.")
                return@execute
            }
            val parameters = localVideoSender!!.parameters
            if (parameters.encodings.size == 0) {
                Log.w(TAG, "RtpParameters are not ready.")
                return@execute
            }
            for (encoding in parameters.encodings) {
                // Null value means no limit.
                encoding.maxBitrateBps = maxBitrateKbps * BPS_IN_KBPS
            }
            if (localVideoSender?.setParameters(parameters) == false) {
                Log.e(TAG, "RtpSender.setParameters failed.")
            }
            Log.d(TAG, "Configured max video bitrate to: $maxBitrateKbps")
        }
    }
    
    private fun reportError(errorMessage: String) {
        Log.e(TAG, "Peerconnection error: $errorMessage")
        executor.execute {
            if (!isError) {
                events?.onPeerConnectionError(errorMessage)
                isError = true
            }
        }
    }

    private fun createAudioTrack(): AudioTrack? {
        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        localAudioTrack?.setEnabled(enableAudio)
        return localAudioTrack
    }

    private fun createVideoTrack(capturer: VideoCapturer?): VideoTrack? {
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
        videoSource = factory?.createVideoSource(false)
        capturer?.initialize(surfaceTextureHelper, appContext, videoSource?.capturerObserver)
        capturer?.startCapture(videoWidth, videoHeight, videoFps)
        localVideoTrack = factory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        localVideoTrack?.setEnabled(renderVideo)
        localVideoTrack?.addSink(localRender)
        return localVideoTrack
    }

    private fun findVideoSender() {
        for (sender in peerConnection?.senders ?: emptyList()) {
            if (sender.track() != null) {
                val trackType = sender.track()?.kind()
                if (trackType == PeerConnectionClient.VIDEO_TRACK_TYPE) {
                    Log.d(TAG, "Found video sender.")
                    localVideoSender = sender
                }
            }
        }
    }

    // Returns the remote VideoTrack, assuming there is only one.
    private fun getRemoteVideoTrack(): VideoTrack? {
        for (transceiver in peerConnection?.transceivers ?: emptyList()) {
            val track = transceiver.receiver.track()
            if (track is VideoTrack) {
                return track
            }
        }
        return null
    }

    private fun setStartBitrate(
        codec: String,
        isVideoCodec: Boolean,
        sdpDescription: String,
        bitrateKbps: Int
    ): String? {
        val lines =
            sdpDescription.split("\r\n".toRegex()).toTypedArray()
        var rtpmapLineIndex = -1
        var sdpFormatUpdated = false
        var codecRtpMap: String? = null
        // Search for codec rtpmap in format
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        var regex = "^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$"
        var codecPattern = Pattern.compile(regex)
        for (i in lines.indices) {
            val codecMatcher = codecPattern.matcher(lines[i])
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1)
                rtpmapLineIndex = i
                break
            }
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for $codec codec")
            return sdpDescription
        }
        Log.d(
            TAG,
            "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex]
        )

        // Check if a=fmtp string already exist in remote SDP for this codec and
        // update it with new bitrate parameter.
        regex = "^a=fmtp:$codecRtpMap \\w+=\\d+.*[\r]?$"
        codecPattern = Pattern.compile(regex)
        for (i in lines.indices) {
            val codecMatcher = codecPattern.matcher(lines[i])
            if (codecMatcher.matches()) {
                Log.d(TAG, "Found " + codec + " " + lines[i])
                if (isVideoCodec) {
                    lines[i] += "; " + PeerConnectionClient.VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps
                } else {
                    lines[i] += "; " + PeerConnectionClient.AUDIO_CODEC_PARAM_BITRATE + "=" + bitrateKbps * 1000
                }
                Log.d(
                    TAG,
                    "Update remote SDP line: " + lines[i]
                )
                sdpFormatUpdated = true
                break
            }
        }
        val newSdpDescription = StringBuilder()
        for (i in lines.indices) {
            newSdpDescription.append(lines[i]).append("\r\n")
            // Append new a=fmtp line if no such line exist for a codec.
            if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                var bitrateSet: String
                bitrateSet = if (isVideoCodec) {
                    "a=fmtp:$codecRtpMap $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                } else {
                    ("a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "="
                            + bitrateKbps * 1000)
                }
                Log.d(TAG, "Add remote SDP line: $bitrateSet")
                newSdpDescription.append(bitrateSet).append("\r\n")
            }
        }
        return newSdpDescription.toString()
    }

    /** Returns the line number containing "m=audio|video", or -1 if no such line exists.  */
    private fun findMediaDescriptionLine(isAudio: Boolean, sdpLines: Array<String>): Int {
        val mediaDescription = if (isAudio) "m=audio " else "m=video "
        for (i in sdpLines.indices) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i
            }
        }
        return -1
    }

    private fun joinString(
        s: Iterable<CharSequence?>,
        delimiter: String,
        delimiterAtEnd: Boolean
    ): String? {
        val iter = s.iterator()
        if (!iter.hasNext()) {
            return ""
        }
        val buffer = iter.next()?.let { StringBuilder(it) }
        while (iter.hasNext()) {
            buffer?.append(delimiter)?.append(iter.next())
        }
        if (delimiterAtEnd) {
            buffer?.append(delimiter)
        }
        return buffer.toString()
    }

    private fun movePayloadTypesToFront(
        preferredPayloadTypes: List<String?>, mLine: String
    ): String? {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        val origLineParts = listOf(*mLine.split(" ".toRegex()).toTypedArray())
        if (origLineParts.size <= 3) {
            Log.e(TAG, "Wrong SDP media description format: $mLine")
            return null
        }
        val header: List<String?> = origLineParts.subList(0, 3)
        val unpreferredPayloadTypes = ArrayList(origLineParts.subList(3, origLineParts.size))
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes)
        // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
        // types.
        val newLineParts = ArrayList<String?>()
        newLineParts.addAll(header)
        newLineParts.addAll(preferredPayloadTypes)
        newLineParts.addAll(unpreferredPayloadTypes)
        return joinString(newLineParts, " ", false /* delimiterAtEnd */)
    }

    private fun preferCodec(
        sdpDescription: String,
        codec: String,
        isAudio: Boolean
    ): String? {
        val lines = sdpDescription.split("\r\n".toRegex()).toTypedArray()
        val mLineIndex = findMediaDescriptionLine(isAudio, lines)
        if (mLineIndex == -1) {
            Log.w(TAG, "No mediaDescription line, so can't prefer $codec")
            return sdpDescription
        }
        // A list with all the payload types with name |codec|. The payload types are integers in the
        // range 96-127, but they are stored as strings here.
        val codecPayloadTypes = ArrayList<String?>()
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        val codecPattern = Pattern.compile("^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$")
        for (line in lines) {
            val codecMatcher = codecPattern.matcher(line)
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1))
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "No payload types with name $codec")
            return sdpDescription
        }
        val newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]) ?: return sdpDescription
        Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine)
        lines[mLineIndex] = newMLine
        return joinString(
            listOf(*lines),
            "\r\n",
            true /* delimiterAtEnd */
        )
    }

    private fun drainCandidates() {
        if (queuedRemoteCandidates != null) {
            Log.d(TAG, "Add ${queuedRemoteCandidates?.size} remote candidates")
            for (candidate in queuedRemoteCandidates ?: mutableListOf()) {
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteCandidates = null
        }
    }

    private fun switchCameraInternal() {
        if (videoCapturer is CameraVideoCapturer) {
            if (!isVideoCallEnabled() || isError) {
                Log.e(TAG, "Failed to switch camera. Video: ${isVideoCallEnabled()}. Error: $isError")
                return  // No video is sent or only one camera is available or error happened.
            }
            Log.d(TAG, "Switch camera")
            val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
            cameraVideoCapturer.switchCamera(null)
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera")
        }
    }

    fun switchCamera() {
        executor.execute { switchCameraInternal() }
    }

    fun changeCaptureFormat(width: Int, height: Int, framerate: Int) {
        executor.execute {
            changeCaptureFormatInternal(width, height, framerate)
        }
    }

    private fun changeCaptureFormatInternal(width: Int, height: Int, framerate: Int) {
        if (!isVideoCallEnabled() || isError || videoCapturer == null) {
            Log.e(TAG, "Failed to change capture format. Video: ${isVideoCallEnabled()}. Error: $isError")
            return
        }
        Log.d(TAG, "changeCaptureFormat: $width" + "x" + height + "@" + framerate)
        videoSource?.adaptOutputFormat(width, height, framerate)
    }

    // Implementation detail: observe ICE & stream changes and react accordingly.
    private inner class PCObserver : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            executor.execute {
                events?.onIceCandidate(candidate)
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            executor.execute {
                events?.onIceCandidatesRemoved(candidates)
            }
        }

        override fun onSignalingChange(newState: SignalingState) {
            Log.d(TAG, "SignalingState: $newState")
        }

        override fun onIceConnectionChange(newState: IceConnectionState) {
            executor.execute {
                Log.d(TAG, "IceConnectionState: $newState")
                when (newState) {
                    IceConnectionState.CONNECTED -> {
                        events?.onIceConnected()
                    }
                    IceConnectionState.DISCONNECTED -> {
                        events?.onIceDisconnected()
                    }
                    IceConnectionState.FAILED -> {
                        reportError("ICE connection failed.")
                    }
                    else -> {
                    }
                }
            }
        }

        override fun onConnectionChange(newState: PeerConnectionState) {
            executor.execute {
                Log.d(TAG, "PeerConnectionState: $newState")
                when (newState) {
                    PeerConnectionState.CONNECTED -> {
                        events?.onConnected()
                    }
                    PeerConnectionState.DISCONNECTED -> {
                        events?.onDisconnected()
                    }
                    PeerConnectionState.FAILED -> {
                        reportError("DTLS connection failed.")
                    }
                    else -> {
                    }
                }
            }
        }

        override fun onIceGatheringChange(newState: IceGatheringState) {
            Log.d(TAG, "IceGatheringState: $newState")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Log.d(TAG, "IceConnectionReceiving changed to $receiving")
        }

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
            Log.d(TAG, "Selected candidate pair changed because: $event")
        }

        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(dc: DataChannel) {
            Log.d(TAG, "New Data channel " + dc.label())
        }

        override fun onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
        }
    }

    // Implementation detail: handle offer creation/signaling and answer setting,
    // as well as adding remote ICE candidates once the answer SDP is set.
    private inner class SDPObserver : SdpObserver {
        override fun onCreateSuccess(origSdp: SessionDescription) {
            if (localSdp != null) {
                reportError("Multiple SDP create.")
                return
            }
            var sdpDescription = origSdp.description
            if (preferIsac) {
                sdpDescription = preferCodec(
                    sdpDescription,
                    AUDIO_CODEC_ISAC,
                    true
                )
            }
            if (isVideoCallEnabled()) {
                sdpDescription = preferCodec(
                    sdpDescription,
                    getSdpVideoCodecName(peerConnectionParams),
                    false
                )
            }
            val sdp = SessionDescription(origSdp.type, sdpDescription)
            localSdp = sdp
            executor.execute {
                if (peerConnection != null && !isError) {
                    Log.d(TAG, "Set local SDP from " + sdp.type)
                    peerConnection?.setLocalDescription(sdpObserver, sdp)
                }
            }
        }

        override fun onSetSuccess() {
            executor.execute {
                if (peerConnection == null || isError) {
                    return@execute
                }
                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (peerConnection?.remoteDescription == null) {
                        // We've just set our local SDP so time to send it.
                        Log.d(TAG, "Local SDP set succesfully")
                        localSdp?.let { events?.onLocalDescription(it) }
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        Log.d(TAG, "Remote SDP set succesfully")
                        drainCandidates()
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (peerConnection?.localDescription != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        Log.d(TAG, "Local SDP set succesfully")
                        localSdp?.let { events?.onLocalDescription(it) }
                        drainCandidates()
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        Log.d(TAG, "Remote SDP set succesfully")
                    }
                }
            }
        }

        override fun onCreateFailure(error: String) {
            reportError("createSDP error: $error")
        }

        override fun onSetFailure(error: String) {
            reportError("setSDP error: $error")
        }
    }

}