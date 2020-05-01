package q19.kenes_widget.webrtc

import android.content.Context
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.PeerConnection.Observer
import org.webrtc.voiceengine.WebRtcAudioManager
import org.webrtc.voiceengine.WebRtcAudioUtils
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.regex.Pattern

/**
 * Peer connection client implementation.
 *
 * All public methods are routed to local looper thread.
 * All PeerConnectionEvents callbacks are invoked from the same looper thread.
 * This class is a singleton.
 */
class PeerConnectionClient private constructor() {

    companion object {
        private const val TAG = "LOL"

        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
        const val VIDEO_TRACK_TYPE = "video"
        private const val VIDEO_CODEC_VP8 = "VP8"
        private const val VIDEO_CODEC_VP9 = "VP9"
        private const val VIDEO_CODEC_H264 = "H264"
        private const val AUDIO_CODEC_OPUS = "opus"
        private const val AUDIO_CODEC_ISAC = "ISAC"
        private const val VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate"
        private const val VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03/Enabled/"
        private const val AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate"
        private const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
        private const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        private const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        private const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
        private const val AUDIO_LEVEL_CONTROL_CONSTRAINT = "levelControl"
        private const val DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement"
        private const val OFFER_TO_RECEIVE_AUDIO_CONSTRAINT = "OfferToReceiveAudio"
        private const val OFFER_TO_RECEIVE_VIDEO_CONSTRAINT = "OfferToReceiveVideo"
        private const val HD_VIDEO_WIDTH = 1280
        private const val HD_VIDEO_HEIGHT = 720
        private const val BPS_IN_KBPS = 1000

        private const val DEFAULT_VIDEO_WIDTH = 0
        private const val DEFAULT_VIDEO_HEIGHT = 0
        private const val DEFAULT_VIDEO_FPS = 0

        val instance = PeerConnectionClient()

        private fun setStartBitrate(
            codec: String,
            isVideoCodec: Boolean,
            sdpDescription: String,
            bitrateKbps: Int
        ): String {
            val lines = sdpDescription.split("\r\n").toTypedArray()
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
            Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex])

            // Check if a=fmtp string already exist in remote SDP for this codec and
            // update it with new bitrate parameter.
            regex = "^a=fmtp:$codecRtpMap \\w+=\\d+.*[\r]?$"
            codecPattern = Pattern.compile(regex)
            for (i in lines.indices) {
                val codecMatcher = codecPattern.matcher(lines[i])
                if (codecMatcher.matches()) {
                    Log.d(TAG, "Found " + codec + " " + lines[i])
                    if (isVideoCodec) {
                        lines[i] += "; $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                    } else {
                        lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + bitrateKbps * 1000
                    }
                    Log.d(TAG, "Update remote SDP line: " + lines[i])
                    sdpFormatUpdated = true
                    break
                }
            }
            val newSdpDescription = StringBuilder()
            for (i in lines.indices) {
                newSdpDescription.append(lines[i]).append("\r\n")
                // Append new a=fmtp line if no such line exist for a codec.
                if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                    val bitrateSet: String = if (isVideoCodec) {
                        "a=fmtp:$codecRtpMap $VIDEO_CODEC_PARAM_START_BITRATE=$bitrateKbps"
                    } else {
                        ("a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "=" + bitrateKbps * 1000)
                    }
                    Log.d(TAG, "Add remote SDP line: $bitrateSet")
                    newSdpDescription.append(bitrateSet).append("\r\n")
                }
            }
            return newSdpDescription.toString()
        }

        private fun preferCodec(
            sdpDescription: String,
            codec: String?,
            isAudio: Boolean
        ): String {
            val lines = sdpDescription.split("\r\n").toTypedArray()
            var mLineIndex = -1
            var codecRtpMap: String? = null
            // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
            val regex = "^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$"
            val codecPattern = Pattern.compile(regex)
            var mediaDescription = "m=video "
            if (isAudio) {
                mediaDescription = "m=audio "
            }
            var i = 0
            while (i < lines.size && (mLineIndex == -1 || codecRtpMap == null)) {
                if (lines[i].startsWith(mediaDescription)) {
                    mLineIndex = i
                    i++
                    continue
                }
                val codecMatcher = codecPattern.matcher(lines[i])
                if (codecMatcher.matches()) {
                    codecRtpMap = codecMatcher.group(1)
                }
                i++
            }
            if (mLineIndex == -1) {
                Log.w(TAG, "No $mediaDescription line, so can't prefer $codec")
                return sdpDescription
            }
            if (codecRtpMap == null) {
                Log.w(TAG, "No rtpmap for $codec")
                return sdpDescription
            }
            Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at " + lines[mLineIndex])
            val origMLineParts = lines[mLineIndex].split(" ").toTypedArray()
            if (origMLineParts.size > 3) {
                val newMLine = StringBuilder()
                var origPartIndex = 0
                // Format is: m=<media> <port> <proto> <fmt> ...
                newMLine.append(origMLineParts[origPartIndex++]).append(" ")
                newMLine.append(origMLineParts[origPartIndex++]).append(" ")
                newMLine.append(origMLineParts[origPartIndex++]).append(" ")
                newMLine.append(codecRtpMap)
                while (origPartIndex < origMLineParts.size) {
                    if (origMLineParts[origPartIndex] != codecRtpMap) {
                        newMLine.append(" ").append(origMLineParts[origPartIndex])
                    }
                    origPartIndex++
                }
                lines[mLineIndex] = newMLine.toString()
                Log.d(TAG, "Change media description: " + lines[mLineIndex])
            } else {
                Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex])
            }
            val newSdpDescription = StringBuilder()
            for (line in lines) {
                newSdpDescription.append(line).append("\r\n")
            }
            return newSdpDescription.toString()
        }
    }

    // Executor thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    private var executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val pcObserver = PCObserver()
    private val sdpObserver = SDPObserver()

    private var context: Context? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    var options: PeerConnectionFactory.Options? = null
    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null
    var isVideoCallEnabled = false
        private set
    private var preferIsac = false
    private var preferredVideoCodec: String? = null
    private var videoCapturerStopped = false
    private var isError = false
    private var statsTimer: Timer? = null
    private var localRender: VideoRenderer.Callbacks? = null
    private var remoteRenders: List<VideoRenderer.Callbacks>? = null
    private var signalingParameters: AppRTCClient.SignalingParameters? = null
    private var pcConstraints: MediaConstraints? = null
    private var videoWidth = DEFAULT_VIDEO_WIDTH
    private var videoHeight = DEFAULT_VIDEO_HEIGHT
    private var videoFps = DEFAULT_VIDEO_FPS
    private var audioConstraints: MediaConstraints? = null
    private var aecDumpFileDescriptor: ParcelFileDescriptor? = null
    private var sdpMediaConstraints: MediaConstraints? = null
    private var peerConnectionParameters: PeerConnectionParameters? = null

    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private var queuedRemoteCandidates: LinkedList<IceCandidate>? = null
    private var events: PeerConnectionEvents? = null
    private var isInitiator = false
    private var localSdp : SessionDescription? = null // either offer or answer SDP
    private var mediaStream: MediaStream? = null
    private var videoCapturer: VideoCapturer? = null

    // enableVideo is set to true if video should be rendered and sent.
    private var renderVideo = false
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var localVideoSender: RtpSender? = null

    // enableAudio is set to true if audio should be sent.
    private var enableAudio = false
    private var localAudioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null
    private var dataChannelEnabled = false

    /**
     * Peer connection parameters.
     */
    class DataChannelParameters(
        val ordered: Boolean,
        val maxRetransmitTimeMs: Int,
        val maxRetransmits: Int,
        val protocol: String,
        val negotiated: Boolean,
        val id: Int
    )

    /**
     * Peer connection parameters.
     */
    class PeerConnectionParameters(
        val videoCallEnabled: Boolean,
        val loopback: Boolean,
        val tracing: Boolean,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoFps: Int,
        val videoMaxBitrate: Int,
        val videoCodec: String?,
        val videoCodecHwAcceleration: Boolean,
        val videoFlexfecEnabled: Boolean,
        val audioStartBitrate: Int,
        val audioCodec: String?,
        val noAudioProcessing: Boolean,
        val aecDump: Boolean,
        val useOpenSLES: Boolean,
        val disableBuiltInAEC: Boolean,
        val disableBuiltInAGC: Boolean,
        val disableBuiltInNS: Boolean,
        val enableLevelControl: Boolean,
        val dataChannelParameters: DataChannelParameters? = null
    ) {

        companion object {
            fun createDefault(): PeerConnectionParameters {
                return PeerConnectionParameters(
                    videoCallEnabled = true,
                    loopback = false,
                    tracing = false,
                    videoWidth = 0,
                    videoHeight = 0,
                    videoFps = 0,
                    videoMaxBitrate = 0,
                    videoCodec = "VP8",
                    videoCodecHwAcceleration = true,
                    videoFlexfecEnabled = false,
                    audioStartBitrate = 0,
                    audioCodec = "OPUS",
                    noAudioProcessing = false,
                    aecDump = false,
                    useOpenSLES = false,
                    disableBuiltInAEC = false,
                    disableBuiltInAGC = false,
                    disableBuiltInNS = false,
                    enableLevelControl = false
                )
            }
        }

    }

    /**
     * Peer connection events.
     */
    interface PeerConnectionEvents {
        /**
         * Callback fired once local SDP is created and set.
         */
        fun onLocalDescription(sdp: SessionDescription?)

        /**
         * Callback fired once local Ice candidate is generated.
         */
        fun onIceCandidate(candidate: IceCandidate?)

        /**
         * Callback fired once local ICE candidates are removed.
         */
        fun onIceCandidatesRemoved(candidates: Array<IceCandidate>?)

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        fun onIceConnected()

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        fun onIceDisconnected()

        /**
         * Callback fired once peer connection is closed.
         */
        fun onPeerConnectionClosed()

        /**
         * Callback fired once peer connection statistics is ready.
         */
        fun onPeerConnectionStatsReady(reports: Array<StatsReport?>?)

        /**
         * Callback fired once peer connection error happened.
         */
        fun onPeerConnectionError(description: String?)
    }

    fun setPeerConnectionFactoryOptions(options: PeerConnectionFactory.Options?) {
        this.options = options
    }

    fun createPeerConnectionFactory(
        context: Context,
        peerConnectionParameters: PeerConnectionParameters, events: PeerConnectionEvents?
    ) {
        this.peerConnectionParameters = peerConnectionParameters
        this.events = events
        isVideoCallEnabled = peerConnectionParameters.videoCallEnabled
        dataChannelEnabled = peerConnectionParameters.dataChannelParameters != null
        // Reset variables to initial states.
        this.context = null
        factory = null
        peerConnection = null
        preferIsac = false
        videoCapturerStopped = false
        isError = false
        queuedRemoteCandidates = null
        localSdp = null // either offer or answer SDP
        mediaStream = null
        videoCapturer = null
        renderVideo = true
        localVideoTrack = null
        remoteVideoTrack = null
        localVideoSender = null
        enableAudio = true
        localAudioTrack = null
        statsTimer = Timer()
        executor.execute { createPeerConnectionFactoryInternal(context) }
    }

    fun createPeerConnection(
        renderEGLContext: EglBase.Context,
        localRender: VideoRenderer.Callbacks?,
        remoteRenders: List<VideoRenderer.Callbacks>?,
        videoCapturer: VideoCapturer?,
        signalingParameters: AppRTCClient.SignalingParameters?
    ) {
        if (peerConnectionParameters == null) {
            logError("Creating peer connection without initializing factory.")
            return
        }
        this.localRender = localRender
        this.remoteRenders = remoteRenders
        this.videoCapturer = videoCapturer
        this.signalingParameters = signalingParameters
        executor.execute {
            try {
                createMediaConstraintsInternal()
                createPeerConnectionInternal(renderEGLContext)
            } catch (e: Exception) {
                reportError("Failed to create peer connection: " + e.message)
                throw e
            }
        }
    }

    fun close() {
        executor.execute { closeInternal() }
    }

    private fun createPeerConnectionFactoryInternal(context: Context) {
        PeerConnectionFactory.initializeInternalTracer()
        if (peerConnectionParameters!!.tracing) {
            val path =
                Environment.getExternalStorageDirectory().absolutePath + File.separator + "webrtc-trace.txt"
            PeerConnectionFactory.startInternalTracingCapture(path)
        }
        logDebug("Create peer connection factory. Use video: " + peerConnectionParameters?.videoCallEnabled)
        isError = false

        // Initialize field trials.
        if (peerConnectionParameters!!.videoFlexfecEnabled) {
            PeerConnectionFactory.initializeFieldTrials(VIDEO_FLEXFEC_FIELDTRIAL)
            logDebug("Enable FlexFEC field trial.")
        } else {
            PeerConnectionFactory.initializeFieldTrials("")
        }

        // Check preferred video codec.
        preferredVideoCodec = VIDEO_CODEC_VP8
        if (isVideoCallEnabled && peerConnectionParameters?.videoCodec != null) {
            if (peerConnectionParameters?.videoCodec == VIDEO_CODEC_VP9) {
                preferredVideoCodec = VIDEO_CODEC_VP9
            } else if (peerConnectionParameters?.videoCodec == VIDEO_CODEC_H264) {
                preferredVideoCodec = VIDEO_CODEC_H264
            }
        }
        logDebug("Preferred video codec: $preferredVideoCodec")

        // Check if ISAC is used by default.
        preferIsac =
            (peerConnectionParameters?.audioCodec != null && peerConnectionParameters?.audioCodec == AUDIO_CODEC_ISAC)

        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters!!.useOpenSLES) {
            logDebug("Disable OpenSL ES audio even if device supports it")
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */)
        } else {
            logDebug("Allow OpenSL ES audio if device supports it")
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false)
        }
        if (peerConnectionParameters!!.disableBuiltInAEC) {
            logDebug("Disable built-in AEC even if device supports it")
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true)
        } else {
            logDebug("Enable built-in AEC if device supports it")
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false)
        }
        if (peerConnectionParameters!!.disableBuiltInAGC) {
            logDebug("Disable built-in AGC even if device supports it")
            WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true)
        } else {
            logDebug("Enable built-in AGC if device supports it")
            WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(false)
        }
        if (peerConnectionParameters!!.disableBuiltInNS) {
            logDebug("Disable built-in NS even if device supports it")
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true)
        } else {
            logDebug("Enable built-in NS if device supports it")
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false)
        }

        // Create peer connection factory.
        if (!PeerConnectionFactory.initializeAndroidGlobals(
                context,
                true,
                true,
                peerConnectionParameters!!.videoCodecHwAcceleration
            )
        ) {
            events?.onPeerConnectionError("Failed to initializeAndroidGlobals")
        }
        if (options != null) {
            logDebug("Factory networkIgnoreMask option: " + options?.networkIgnoreMask)
        }
        this.context = context
        factory = PeerConnectionFactory(options)
        logDebug("Peer connection factory created.")
    }

    private fun createMediaConstraintsInternal() {
        // Create peer connection constraints.
        pcConstraints = MediaConstraints()
        // Enable DTLS for normal calls and disable for loopback calls.
        if (peerConnectionParameters != null && peerConnectionParameters!!.loopback) {
            pcConstraints?.optional?.add(
                MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false")
            )
        } else {
            pcConstraints?.optional?.add(
                MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true")
            )
        }

        // Check if there is a camera on device and disable video call if not.
        if (videoCapturer == null) {
            logWarn("No camera on device. Switch to audio only call.")
            isVideoCallEnabled = false
        }
        // Create video constraints if video call is enabled.
        if (isVideoCallEnabled) {
            videoWidth = peerConnectionParameters?.videoWidth ?: DEFAULT_VIDEO_WIDTH
            videoHeight = peerConnectionParameters?.videoHeight ?: DEFAULT_VIDEO_HEIGHT
            videoFps = peerConnectionParameters?.videoFps ?: DEFAULT_VIDEO_FPS

            // If video resolution is not specified, default to HD.
            if (videoWidth == DEFAULT_VIDEO_WIDTH || videoHeight == DEFAULT_VIDEO_HEIGHT) {
                videoWidth = HD_VIDEO_WIDTH
                videoHeight = HD_VIDEO_HEIGHT
            }

            // If fps is not specified, default to 30.
            if (videoFps == DEFAULT_VIDEO_FPS) {
                videoFps = 30
            }
            Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps)
        }

        // Create audio constraints.
        audioConstraints = MediaConstraints()
        // added for audio performance measurements
        if (peerConnectionParameters != null && peerConnectionParameters!!.noAudioProcessing) {
            logDebug("Disabling audio processing")
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
        if (peerConnectionParameters != null && peerConnectionParameters!!.enableLevelControl) {
            logDebug("Enabling level control.")
            audioConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(AUDIO_LEVEL_CONTROL_CONSTRAINT, "true")
            )
        }
        // Create SDP constraints.
        sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints?.mandatory?.add(
            MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO_CONSTRAINT, "true")
        )
        if (isVideoCallEnabled || (peerConnectionParameters != null && peerConnectionParameters!!.loopback)) {
            sdpMediaConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO_CONSTRAINT, "true")
            )
        } else {
            sdpMediaConstraints?.mandatory?.add(
                MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO_CONSTRAINT, "false")
            )
        }
    }

    private fun createPeerConnectionInternal(renderEGLContext: EglBase.Context) {
        if (factory == null || isError) {
            logError("Peerconnection factory is not created")
            return
        }
        logDebug("Create peer connection.")
        logDebug("PCConstraints: $pcConstraints")
        queuedRemoteCandidates = LinkedList()
        if (isVideoCallEnabled) {
            logDebug("EGLContext: $renderEGLContext")
            factory?.setVideoHwAccelerationOptions(renderEGLContext, renderEGLContext)
        }
        val rtcConfig = RTCConfiguration(signalingParameters?.iceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        rtcConfig.keyType = KeyType.ECDSA
        peerConnection = factory?.createPeerConnection(rtcConfig, pcConstraints, pcObserver)
        if (dataChannelEnabled) {
            val init = DataChannel.Init()
            peerConnectionParameters?.dataChannelParameters?.let { dataChannelParameters ->
                init.ordered = dataChannelParameters.ordered
                init.negotiated = dataChannelParameters.negotiated
                init.maxRetransmits = dataChannelParameters.maxRetransmits
                init.maxRetransmitTimeMs = dataChannelParameters.maxRetransmitTimeMs
                init.id = dataChannelParameters.id
                init.protocol = peerConnectionParameters?.dataChannelParameters?.protocol
            }
            dataChannel = peerConnection?.createDataChannel("ApprtcDemo data", init)
        }
        isInitiator = false

        // Set default WebRTC tracing and INFO libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT))
        Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
        mediaStream = factory?.createLocalMediaStream("ARDAMS")
        if (isVideoCallEnabled) {
            mediaStream?.addTrack(createVideoTrack(videoCapturer))
        }
        mediaStream?.addTrack(createAudioTrack())
        peerConnection?.addStream(mediaStream)
        if (isVideoCallEnabled) {
            findVideoSender()
        }
        if (peerConnectionParameters != null && peerConnectionParameters!!.aecDump) {
            try {
                aecDumpFileDescriptor = ParcelFileDescriptor.open(
                    File(Environment.getExternalStorageDirectory().path + File.separator + "Download/audio.aecdump"),
                    ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                )
                factory?.startAecDump(aecDumpFileDescriptor!!.fd, -1)
            } catch (e: IOException) {
                logError("Can not open aecdump file: $e")
            }
        }
        logDebug("Peer connection created.")
    }

    private fun closeInternal() {
        if (factory != null && peerConnectionParameters != null && peerConnectionParameters!!.aecDump) {
            factory?.stopAecDump()
        }

        logDebug("Closing peer connection.")

        statsTimer?.cancel()

        dataChannel?.dispose()
        dataChannel = null

        peerConnection?.dispose()
        peerConnection = null

        logDebug("Closing audio source.")
        audioSource?.dispose()
        audioSource = null

        logDebug("Stopping capture.")
        try {
            videoCapturer?.stopCapture()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        videoCapturerStopped = true
        videoCapturer?.dispose()
        videoCapturer = null

        logDebug("Closing video source.")
        videoSource?.dispose()
        videoSource = null

        logDebug("Closing peer connection factory.")
        factory?.dispose()
        factory = null

        options = null

        logDebug("Closing peer connection done.")

        events?.onPeerConnectionClosed()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    val isHDVideo: Boolean
        get() = if (!isVideoCallEnabled) {
            false
        } else videoWidth * videoHeight >= 1280 * 720

    private val stats: Unit
        private get() {
            if (peerConnection == null || isError) {
                return
            }
            val success = peerConnection?.getStats({ reports: Array<StatsReport?>? ->
                events?.onPeerConnectionStatsReady(reports)
            }, null) ?: false
            if (!success) {
                logError("getStats() returns false!")
            }
        }

    fun enableStatsEvents(enable: Boolean, periodMs: Int) {
        if (enable) {
            try {
                statsTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        executor.execute { stats }
                    }
                }, 0, periodMs.toLong())
            } catch (e: Exception) {
                logError("Can not schedule statistics timer: $e")
            }
        } else {
            statsTimer?.cancel()
        }
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
                logDebug("PC Create OFFER")
                isInitiator = true
                peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
            }
        }
    }

    fun createAnswer() {
        executor.execute {
            if (peerConnection != null && !isError) {
                logDebug("PC create ANSWER")
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
        executor.execute(Runnable {
            if (peerConnection == null || isError) {
                return@Runnable
            }
            // Drain the queued remote candidates if there is any so that
            // they are processed in the proper order.
            drainCandidates()
            peerConnection?.removeIceCandidates(candidates)
        })
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        executor.execute(object : Runnable {
            override fun run() {
                if (peerConnection == null || isError) {
                    return
                }
                var sdpDescription = sdp.description
                if (preferIsac) {
                    sdpDescription = preferCodec(
                        sdpDescription,
                        AUDIO_CODEC_ISAC,
                        true
                    )
                }
                if (isVideoCallEnabled) {
                    sdpDescription = preferCodec(
                        sdpDescription,
                        preferredVideoCodec,
                        false
                    )
                }
                if (peerConnectionParameters != null && peerConnectionParameters!!.audioStartBitrate > 0) {
                    sdpDescription = setStartBitrate(
                        AUDIO_CODEC_OPUS,
                        false,
                        sdpDescription,
                        peerConnectionParameters!!.audioStartBitrate
                    )
                }
                logDebug("Set remote SDP.")
                val sdpRemote = SessionDescription(sdp.type, sdpDescription)
                peerConnection?.setRemoteDescription(sdpObserver, sdpRemote)
            }
        })
    }

    fun stopVideoSource() {
        executor.execute {
            if (videoCapturer != null && !videoCapturerStopped) {
                logDebug("Stop video source.")
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
                logDebug("Restart video source.")
                videoCapturer?.startCapture(videoWidth, videoHeight, videoFps)
                videoCapturerStopped = false
            }
        }
    }

    fun setVideoMaxBitrate(maxBitrateKbps: Int?) {
        executor.execute(Runnable {
            if (peerConnection == null || localVideoSender == null || isError) {
                return@Runnable
            }
            logDebug("Requested max video bitrate: $maxBitrateKbps")
            if (localVideoSender == null) {
                logWarn("Sender is not ready.")
                return@Runnable
            }
            val parameters = localVideoSender?.parameters
            if (parameters != null) {
                if (parameters.encodings.size == 0) {
                    logWarn("RtpParameters are not ready.")
                    return@Runnable
                }
                for (encoding in parameters.encodings) {
                    // Null value means no limit.
                    encoding.maxBitrateBps =
                        if (maxBitrateKbps == null) null else maxBitrateKbps * BPS_IN_KBPS
                }
                if (!localVideoSender!!.setParameters(parameters)) {
                    logError("RtpSender.setParameters failed.")
                }
            }
            logDebug("Configured max video bitrate to: $maxBitrateKbps")
        })
    }

    private fun reportError(errorMessage: String) {
        logError("Peerconnection error: $errorMessage")
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
        videoSource = factory?.createVideoSource(capturer)
        capturer?.startCapture(videoWidth, videoHeight, videoFps)
        localVideoTrack = factory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        localVideoTrack?.setEnabled(renderVideo)
        localVideoTrack?.addRenderer(VideoRenderer(localRender))
        return localVideoTrack
    }

    private fun findVideoSender() {
        peerConnection?.let { peerConnection ->
            for (sender in peerConnection.senders) {
                if (sender.track() != null) {
                    val trackType = sender.track().kind()
                    if (trackType == VIDEO_TRACK_TYPE) {
                        logDebug("Found video sender.")
                        localVideoSender = sender
                    }
                }
            }
        }
    }

    private fun drainCandidates() {
        if (queuedRemoteCandidates != null) {
            logDebug("Add " + queuedRemoteCandidates?.size + " remote candidates")
            for (candidate in queuedRemoteCandidates!!) {
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteCandidates = null
        }
    }

    private fun switchCameraInternal() {
        if (videoCapturer is CameraVideoCapturer) {
            if (!isVideoCallEnabled || isError || videoCapturer == null) {
                logError("Failed to switch camera. Video: $isVideoCallEnabled. Error : $isError")
                return  // No video is sent or only one camera is available or error happened.
            }
            logDebug("Switch camera")
            val cameraVideoCapturer = videoCapturer as? CameraVideoCapturer?
            cameraVideoCapturer?.switchCamera(null)
        } else {
            logDebug("Will not switch camera, video caputurer is not a camera")
        }
    }

    fun switchCamera() {
        executor.execute { switchCameraInternal() }
    }

    fun changeCaptureFormat(width: Int, height: Int, frameRate: Int) {
        executor.execute { changeCaptureFormatInternal(width, height, frameRate) }
    }

    private fun changeCaptureFormatInternal(width: Int, height: Int, framerate: Int) {
        if (!isVideoCallEnabled || isError || videoCapturer == null) {
            logError("Failed to change capture format. Video: $isVideoCallEnabled. Error : $isError")
            return
        }
        logDebug("changeCaptureFormat: " + width + "x" + height + "@" + framerate)
        videoSource?.adaptOutputFormat(width, height, framerate)
    }

    // Implementation detail: observe ICE & stream changes and react accordingly.
    private inner class PCObserver : Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            executor.execute { events?.onIceCandidate(candidate) }
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            executor.execute { events?.onIceCandidatesRemoved(candidates) }
        }

        override fun onSignalingChange(newState: SignalingState) {
            logDebug("SignalingState: $newState")
        }

        override fun onIceConnectionChange(newState: IceConnectionState) {
            executor.execute {
                logDebug("IceConnectionState: $newState")
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

        override fun onIceGatheringChange(newState: IceGatheringState) {
            logDebug("IceGatheringState: $newState")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            logDebug("IceConnectionReceiving changed to $receiving")
        }

        override fun onAddStream(stream: MediaStream) {
            executor.execute(Runnable {
                if (peerConnection == null || isError) {
                    return@Runnable
                }
                if (stream.audioTracks.size > 1 || stream.videoTracks.size > 1) {
                    reportError("Weird-looking stream: $stream")
                    return@Runnable
                }
                if (stream.videoTracks.size == 1) {
                    remoteVideoTrack = stream.videoTracks[0]
                    remoteVideoTrack?.setEnabled(renderVideo)
                    for (remoteRender in remoteRenders!!) {
                        remoteVideoTrack?.addRenderer(VideoRenderer(remoteRender))
                    }
                }
            })
        }

        override fun onRemoveStream(stream: MediaStream) {
            executor.execute { remoteVideoTrack = null }
        }

        override fun onDataChannel(dc: DataChannel) {
            logDebug("New Data channel " + dc.label())
            if (!dataChannelEnabled) return
            dc.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {
                    logDebug("Data channel buffered amount changed: " + dc.label() + ": " + dc.state())
                }

                override fun onStateChange() {
                    logDebug("Data channel state changed: " + dc.label() + ": " + dc.state())
                }

                override fun onMessage(buffer: DataChannel.Buffer) {
                    if (buffer.binary) {
                        logDebug("Received binary msg over $dc")
                        return
                    }
                    val data = buffer.data
                    val bytes = ByteArray(data.capacity())
                    data[bytes]
                    val strData = String(bytes)
                    logDebug("Got msg: $strData over $dc")
                }
            })
        }

        override fun onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
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
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true)
            }
            if (isVideoCallEnabled) {
                sdpDescription = preferCodec(sdpDescription, preferredVideoCodec, false)
            }
            val sdp = SessionDescription(origSdp.type, sdpDescription)
            localSdp = sdp
            executor.execute {
                if (peerConnection != null && !isError) {
                    logDebug("Set local SDP from " + sdp.type)
                    peerConnection?.setLocalDescription(sdpObserver, sdp)
                }
            }
        }

        override fun onSetSuccess() {
            executor.execute(Runnable {
                if (peerConnection == null || isError) {
                    return@Runnable
                }
                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (peerConnection?.remoteDescription == null) {
                        // We've just set our local SDP so time to send it.
                        logDebug("Local SDP set successfully")
                        events?.onLocalDescription(localSdp)
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        logDebug("Remote SDP set successfully")
                        drainCandidates()
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (peerConnection?.localDescription != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        logDebug("Local SDP set successfully")
                        events?.onLocalDescription(localSdp)
                        drainCandidates()
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        logDebug("Remote SDP set successfully")
                    }
                }
            })
        }

        override fun onCreateFailure(error: String) {
            reportError("createSDP error: $error")
        }

        override fun onSetFailure(error: String) {
            reportError("setSDP error: $error")
        }
    }
    
    private fun logDebug(message: String) {
        Log.d(TAG, message)
    }

    private fun logWarn(message: String) {
        logWarn(message)
    }

    private fun logError(message: String) {
        logError(message)
    }

}