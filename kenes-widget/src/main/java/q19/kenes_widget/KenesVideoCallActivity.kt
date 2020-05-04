package q19.kenes_widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.android.synthetic.main.kenes_activity_video_call.*
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import q19.kenes_widget.adapter.ChatAdapter
import q19.kenes_widget.adapter.ChatAdapterItemDecoration
import q19.kenes_widget.adapter.RatingAdapter
import q19.kenes_widget.model.*
import q19.kenes_widget.model.Message
import q19.kenes_widget.network.HttpRequestHandler
import q19.kenes_widget.util.CircleTransformation
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.optJSONArrayAsList
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.hideKeyboard
import q19.kenes_widget.webrtc.SimpleSdpObserver

class KenesVideoCallActivity : AppCompatActivity() {

    companion object {
        const val TAG = "LOL"

        private const val REQUEST_CODE_PERMISSIONS = 111

        const val VIDEO_RESOLUTION_WIDTH = 1280
        const val VIDEO_RESOLUTION_HEIGHT = 720
        const val FPS = 30

        const val AUDIO_TRACK_ID = "ARDAMSa0"
        const val VIDEO_TRACK_ID = "ARDAMSv0"

        private var permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, KenesVideoCallActivity::class.java)
        }
    }

    private var palette = intArrayOf()

    /**
     * Opponent info view variables: [opponentAvatarView], [opponentNameView], [opponentSecondNameView]
     */
    private var opponentAvatarView: AppCompatImageView? = null
    private var opponentNameView: TextView? = null
    private var opponentSecondNameView: TextView? = null

    /**
     * Video call screen view variables: [videoCallView], [videoCallButton], [videoCallInfoView]
     */
    private var videoCallView: LinearLayout? = null
    private var videoCallButton: AppCompatButton? = null
    private var videoCallInfoView: TextView? = null

    /**
     * Footer view variables: [footerView], [inputView], [attachmentButton]
     */
    private var footerView: LinearLayout? = null
    private var inputView: AppCompatEditText? = null
    private var attachmentButton: AppCompatImageButton? = null

    /**
     * Chat view variables: [recyclerView]
     */
    private var recyclerView: RecyclerView? = null

    /**
     * User feedback after dialog view variables: [feedbackView], [titleView], [ratingView], [rateButton]
     */
    private var feedbackView: LinearLayout? = null
    private var titleView: TextView? = null
    private var ratingView: RecyclerView? = null
    private var rateButton: AppCompatButton? = null

    /**
     * Bottom navigation view variables: [homeNavButton], [videoNavButton], [audioNavButton], [infoNavButton]
     */
    private var homeNavButton: AppCompatImageButton? = null
    private var videoNavButton: AppCompatImageButton? = null
    private var audioNavButton: AppCompatImageButton? = null
    private var infoNavButton: AppCompatImageButton? = null

    /**
     * Video dialog view variables: [videoDialogView], [localSurfaceView], [remoteSurfaceView], [hangupButton]
     */
    private var videoDialogView: FrameLayout? = null
    private var localSurfaceView: SurfaceViewRenderer? = null
    private var remoteSurfaceView: SurfaceViewRenderer? = null
    private var hangupButton: AppCompatImageButton? = null

    private var activeNavButtonIndex = 0

    private val navButtons
        get() = listOf(homeNavButton, videoNavButton, audioNavButton, infoNavButton)

    private lateinit var chatAdapter: ChatAdapter

    private var socket: Socket? = null
    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null

    private var iceServers: MutableList<WidgetIceServer> = mutableListOf()

    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null

    private var localMediaStream: MediaStream? = null

    private var videoCapturer: VideoCapturer? = null

    private var localAudioTrack: AudioTrack? = null
    private var remoteAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var configs: Configs = Configs()

    @get:Synchronized
    @set:Synchronized
    private var messages: MutableList<Message> = mutableListOf()

    @get:Synchronized
    @set:Synchronized
    private var activeCategoryChild: Category? = null

    @get:Synchronized
    @set:Synchronized
    private var activeDialog: Dialog? = null

//    private var isCategoriesShown: Boolean = false
//    private var isFilled: Boolean = false

    private var isInitiator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_video_call)

        // TODO: Remove later, exhaustive on PROD
        UrlUtil.setHostname("https://kenes.vlx.kz")
//        UrlUtil.setHostname("https://rtc.vlx.kz")

        /**
         * [Picasso] configuration
         */
        if (Picasso.get() == null) {
            Picasso.setSingletonInstance(Picasso.Builder(this).build())
        }

        palette = resources.getIntArray(R.array.kenes_palette)


        // -------------------------- Binding views -----------------------------------
        /**
         * Bind [R.id.headerView] views: [R.id.opponentAvatarView], [R.id.opponentNameView],
         * [R.id.opponentSecondNameView].
         * Header view for opponent info display.
         */
        opponentAvatarView = findViewById(R.id.opponentAvatarView)
        opponentNameView = findViewById(R.id.opponentNameView)
        opponentSecondNameView = findViewById(R.id.opponentSecondNameView)

        /**
         * Bind [R.id.videoCallView] views: [R.id.videoCallButton], [R.id.videoCallInfoView].
         */
        videoCallView = findViewById(R.id.videoCallView)
        videoCallButton = findViewById(R.id.videoCallButton)
        videoCallInfoView = findViewById(R.id.videoCallInfoView)

        /**
         * Bind [R.id.footerView] views: [R.id.inputView], [R.id.attachmentButton].
         * Footer view for messenger.
         */
        footerView = findViewById(R.id.footerView)
        inputView = findViewById(R.id.inputView)
        attachmentButton = findViewById(R.id.attachmentButton)

        /**
         * Bind [R.id.recyclerView] view.
         * View for chat.
         */
        recyclerView = findViewById(R.id.recyclerView)

        /**
         * Bind [R.id.feedbackView] views: [R.id.titleView], [R.id.ratingView], [R.id.rateButton].
         * Big screen view for user feedback after dialogue with a call agent.
         */
        feedbackView = findViewById(R.id.feedbackView)
        titleView = findViewById(R.id.titleView)
        ratingView = findViewById(R.id.ratingView)
        rateButton = findViewById(R.id.rateButton)

        /**
         * Bind [R.id.navigationView] views: [R.id.homeButton], [R.id.videoButton],
         * [R.id.audioButton], [R.id.videoCallInfoView].
         * Widget navigation buttons.
         */
        homeNavButton = findViewById(R.id.homeButton)
        videoNavButton = findViewById(R.id.videoButton)
        audioNavButton = findViewById(R.id.audioButton)
        infoNavButton = findViewById(R.id.infoButton)

        /**
         * Bind [R.id.videoDialogView] views: [R.id.localSurfaceView], [R.id.remoteSurfaceView],
         * [R.id.hangupButton].
         */
        videoDialogView = findViewById(R.id.videoDialogView)
        localSurfaceView = findViewById(R.id.localSurfaceView)
        remoteSurfaceView = findViewById(R.id.remoteSurfaceView)
        hangupButton = findViewById(R.id.hangupButton)

        // ------------------------------------------------------------------------


        // --------------------- Default screen setups ----------------------------

        /**
         * Default active navigation button [homeNavButton]
         */
        setActiveNavButtonTintColor(homeNavButton)
//        setActiveNavButtonTintColor(videoNavButton)

        /**
         * Default states of views
         */
        rateButton?.isEnabled = false
        bindOpponentData(Configs())
        inputView?.text?.clear()
        activeDialog = null

        // TODO: Remove after attachment upload ability realization
        attachmentButton?.visibility = View.GONE

        videoDialogView?.visibility = View.GONE

        feedbackView?.visibility = View.GONE
        videoCallView?.visibility = View.GONE
        recyclerView?.visibility = View.VISIBLE
        footerView?.visibility = View.VISIBLE

        // ------------------------------------------------------------------------


        /**
         * Configuration of action listeners (click/touch)
         */
        homeNavButton?.setOnClickListener {
            if (feedbackView?.visibility == View.VISIBLE) {
                return@setOnClickListener
            } else {
                activeNavButtonIndex = 0
                updateActiveNavButtonTintColor()

                chatAdapter.clearMessages()
                scrollToTop()

                messages.clear()

                activeCategoryChild = null

//                isCategoriesShown = false
//                isFilled = false

                videoCallButton?.isEnabled = true
                videoCallInfoView?.text = null
                videoCallInfoView?.visibility = View.GONE

                videoCallView?.visibility = View.GONE
                feedbackView?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                footerView?.visibility = View.VISIBLE

                val userDashboard = JSONObject()
                userDashboard.put("action", "get_category_list")
                userDashboard.put("parent_id", 0)
                socket?.emit("user_dashboard", userDashboard)
            }
        }

        videoNavButton?.setOnClickListener {
            activeNavButtonIndex = 1
            updateActiveNavButtonTintColor()

            videoCallButton?.isEnabled = true
            videoCallInfoView?.text = null
            videoCallInfoView?.visibility = View.GONE

            feedbackView?.visibility = View.GONE
            recyclerView?.visibility = View.GONE
            videoCallView?.visibility = View.VISIBLE
            footerView?.visibility = View.VISIBLE
        }

        audioNavButton?.setOnClickListener {
            if (feedbackView?.visibility == View.VISIBLE) {
                return@setOnClickListener
            } else {
                activeNavButtonIndex = 2
                updateActiveNavButtonTintColor()
            }
        }

        infoNavButton?.setOnClickListener {
            if (feedbackView?.visibility == View.VISIBLE) {
                return@setOnClickListener
            } else {
                activeNavButtonIndex = 3
                updateActiveNavButtonTintColor()
            }
        }

        videoCallButton?.setOnClickListener {
            activeDialog = null

            isInitiator = true

//            [BEGIN] Widget video call initialization in kenes.vlx.kz
//            val initialize = JSONObject()
//            initialize.put("video", true)
//            socket?.emit("initialize", initialize)
//            [END] Widget video call initialization in kenes.vlx.kz

//            [BEGIN] Widget video call initialization in rtc.vlx.kz
            val call = JSONObject()
            call.put("to", inputView?.text.toString())
            socket?.emit("call", call)
//            [END] Widget video call initialization in rtc.vlx.kz

            inputView?.text?.clear()

//            videoCallButton?.isEnabled = false
//            videoCallInfoView?.text = null
//            videoCallView?.visibility = View.GONE
//            recyclerView?.visibility = View.VISIBLE
//
//            videoCallInitialize()
//
//            videoDialogView?.visibility = View.VISIBLE
//
//            sendOffer()
        }

        start()

        attachmentButton?.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Не реализовано")
                .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        inputView?.setOnEditorActionListener { v, actionId, keyEvent ->
            logD("setOnEditorActionListener: $actionId, $keyEvent")

            if (actionId == EditorInfo.IME_ACTION_SEND || keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                val text = v.text.toString()

                if (text.isBlank()) {
                    return@setOnEditorActionListener false
                }

                activeCategoryChild = null

                if (chatAdapter.isAllMessagesAreCategory()) {
                    chatAdapter.clearMessages()
                }

                sendUserTextMessage(text)
                inputView?.text?.clear()
                chatAdapter.addNewMessage(Message(Message.Type.SELF, text))
                scrollToBottom()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        inputView?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollToBottom()
            }
        }

        inputView?.setOnClickListener {
            scrollToBottom()
        }

        hangupButton?.setOnClickListener {
            isInitiator = false

//            [BEGIN] Widget active video call hangup in rtc.vlx.kz
            val message = JSONObject()
            val rtc = JSONObject()
            rtc.put("type", "hangup")
            message.put("rtc", rtc)
            socket?.emit("message", message)
//            [END] Widget active video call hangup in rtc.vlx.kz

            closeVideoCall()

            videoDialogView?.visibility = View.GONE
            recyclerView?.visibility = View.GONE

            videoCallButton?.isEnabled = true
            videoCallInfoView?.text = null
            videoCallView?.visibility = View.VISIBLE
        }

        chatAdapter = ChatAdapter(object : ChatAdapter.Callback {
            override fun onReturnBackClicked(category: Category) {
                activeCategoryChild = null

//                val previous = treeByParent(category)

//                val previous = messages.filter {
//                    it.category?.id == category.parentId
//                }
//
//                logD("previous: $previous")
//
//                val isPreviousIsAHome = previous.all { it.category?.home == true }

//                if (isPreviousIsAHome) {
                chatAdapter.setNewMessages(this@KenesVideoCallActivity.messages)
                scrollToTop()
//                } else {
//                    chatAdapter.setNewMessages(previous)
//                    scrollToTop()
//                }
            }

            override fun onCategoryChildClicked(category: Category) {
                activeCategoryChild = category

                if (category.responses.isNotEmpty()) {
                    val userDashboard = JSONObject()
                    userDashboard.put("action", "get_response")
                    userDashboard.put("id", category.responses.first())
                    socket?.emit("user_dashboard", userDashboard)
                } else {
                    val userDashboard = JSONObject()
                    userDashboard.put("action", "get_category_list")
                    userDashboard.put("parent_id", category.id)
                    socket?.emit("user_dashboard", userDashboard)
                }
            }

            override fun onGoToHomeClicked() {
                activeCategoryChild = null

                chatAdapter.setNewMessages(this@KenesVideoCallActivity.messages)
                scrollToTop()
            }
        })
        recyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = chatAdapter
        recyclerView?.addItemDecoration(ChatAdapterItemDecoration(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(R.string.kenes_exit_widget_title)
            .setMessage(R.string.kenes_exit_widget_text)
            .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton(R.string.kenes_no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun start() {
        fetchWidgetConfigs()
        fetchIceServers()

        connectToSignallingServer()
    }

    private fun videoCallInitialize() {
        rootEglBase = EglBase.create()

        runOnUiThread {
            localSurfaceView?.init(rootEglBase?.eglBaseContext, null)
            localSurfaceView?.setEnableHardwareScaler(true)
            localSurfaceView?.setMirror(true)

            remoteSurfaceView?.init(rootEglBase?.eglBaseContext, null)
            remoteSurfaceView?.setEnableHardwareScaler(true)
            remoteSurfaceView?.setMirror(true)
        }

        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true)
        peerConnectionFactory = PeerConnectionFactory(null)
        peerConnectionFactory?.setVideoHwAccelerationOptions(
            rootEglBase?.eglBaseContext,
            rootEglBase?.eglBaseContext
        )

        peerConnection = peerConnectionFactory?.let { createPeerConnection(it) }

        localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")
        localMediaStream?.addTrack(createVideoTrack())
        localMediaStream?.addTrack(createAudioTrack())
        peerConnection?.addStream(localMediaStream)
    }

    private fun createVideoTrack(): VideoTrack? {
        videoCapturer = createVideoCapturer()
        videoSource = peerConnectionFactory?.createVideoSource(videoCapturer)
        videoCapturer?.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS)

        localVideoTrack = peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        localVideoTrack?.setEnabled(true)
        localVideoTrack?.addRenderer(VideoRenderer(localSurfaceView))

        return localVideoTrack
    }

    private fun createAudioTrack(): AudioTrack? {
        audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        localAudioTrack?.setEnabled(true)
        return localAudioTrack
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return if (useCamera2()) {
            createCameraCapturer(Camera2Enumerator(this))
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun useCamera2(): Boolean = Camera2Enumerator.isSupported(this)

    private fun fetchWidgetConfigs() {
        try {
            val asyncTask = HttpRequestHandler(url = UrlUtil.getHostname() + "/configs")
            val response = asyncTask.execute().get()

            val json = if (response.isNullOrBlank()) {
                null
            } else {
                JSONObject(response)
            }

            val configs = json?.optJSONObject("configs")
            val contacts = json?.optJSONObject("contacts")
//            val localBotConfigs = json.optJSONObject("local_bot_configs")

            this.configs.opponent = Configs.Opponent(
                name = configs?.optString("default_operator") ?: "",
                secondName = configs?.optString("title") ?: "",
                avatarUrl = UrlUtil.getStaticUrl(configs?.optString("image")) ?: ""
            )

            contacts?.keys()?.forEach { key ->
                this.configs.contacts.add(Configs.Contact(key, (contacts[key] as? String?) ?: ""))
            }

            this.configs.workingHours = Configs.WorkingHours(
                configs?.optString("message_kk"),
                configs?.optString("message_ru")
            )

            bindOpponentData(this.configs)
        } catch (e: Exception) {
//            e.printStackTrace()
            logD("ERROR! $e")
        }
    }

    private fun fetchIceServers() {
        try {
            val asyncTask = HttpRequestHandler(url = UrlUtil.getHostname() + "/ice_servers")
            val response = asyncTask.execute().get()

            val json = if (response.isNullOrBlank()) {
                null
            } else {
                JSONObject(response)
            }

            val iceServersJson = json?.optJSONArray("ice_servers")

            if (iceServersJson != null) {
                for (i in 0 until iceServersJson.length()) {
                    val iceServerJson = iceServersJson[i] as? JSONObject?

                    this.iceServers.add(WidgetIceServer(
                        url = iceServerJson?.optString("url"),
                        username = iceServerJson?.optString("username"),
                        urls = iceServerJson?.optString("urls"),
                        credential = iceServerJson?.optString("credential")
                    ))
                }

                this.iceServers.forEach { iceServer ->
                    logD("iceServer: $iceServer")
                }
            }
        } catch (e: Exception) {
//            e.printStackTrace()
            logD("ERROR! $e")
        }
    }

    private fun bindOpponentData(configs: Configs) {
        if (!configs.opponent.avatarUrl.isNullOrBlank()) {
            Picasso.get()
                .load(configs.opponent.avatarUrl)
                .fit()
                .centerCrop()
                .transform(CircleTransformation())
                .into(opponentAvatarView)
        } else {
            opponentAvatarView?.setImageDrawable(null)
        }

        opponentNameView?.text = configs.opponent.name
        opponentSecondNameView?.text = configs.opponent.secondName
    }

    private fun connectToSignallingServer() {
        val signallingServerUrl = UrlUtil.getSignallingServerUrl()
        if (signallingServerUrl.isNullOrBlank()) {
            throw NullPointerException("Signalling server url is null.")
        } else {
            socket = IO.socket(signallingServerUrl)
        }

        socket?.on(Socket.EVENT_CONNECT) { args ->
            
            logD("event [EVENT_CONNECT]: $args")

            val userDashboard = JSONObject()
            userDashboard.put("action", "get_category_list")
            userDashboard.put("parent_id", 0)
            socket?.emit("user_dashboard", userDashboard)

//            // TODO: [START] There is no need on PROD
//            val id = "" + (Math.random() * 10000).roundToInt()
//            runOnUiThread {
//                idView.text = id
//            }
//            val reg = JSONObject()
//            reg.put("id", id)
//            socket?.emit("reg", reg)
//            // TODO: [END] There is no need on PROD

        }?.on("open") { args ->

            logD("event [OPEN]: $args")

        }?.on("call") { args ->

            logD("event [CALL]: $args")

            if (args.size == 1) {
                val call = args[0] as? JSONObject?
                logD("JSONObject call: $call")

                if (call != null) {
                    val type = call.optString("type")

                    if (type == "accept") {
                        activeDialog = Dialog(
                            operatorId = call.optString("operator"),
                            instance = call.optString("instance"),
                            media = call.optString("media")
                        )

                        runOnUiThread {
                            videoCallButton?.isEnabled = false
                            videoCallInfoView?.text = null
                            feedbackView?.visibility = View.GONE
                            videoCallView?.visibility = View.GONE
                            recyclerView?.visibility = View.VISIBLE
                        }
                    }
                }
            }

        }?.on("user_queue") { args ->

            logD("event [USER_QUEUE]: $args")

            if (args.size == 1) {
                val userQueueJson = args[0] as? JSONObject?
                logD("userQueueJson: $userQueueJson")
            }

        }?.on("operator_status") { args ->

            logD("event [OPERATOR_STATUS]: $args")

            if (args.size == 1) {
                val operatorStatusJson = args[0] as? JSONObject?
                logD("operatorStatusJson: $operatorStatusJson")
            }

        }?.on("stream") { args ->

            logD("event [STREAM]: $args")

        }?.on("user_chat_init") { args ->

            logD("event [USER_CHAT_INIT]: $args")

            if (args.size == 1) {
                val chatInitJson = args[0] as? JSONObject?
                logD("chatInitJson: $chatInitJson")
            }

        }?.on("category_list") { args ->

            if (args.size == 1) {
                val categoryList = args[0] as? JSONObject?
//                logD("categoryListJson: $categoryList")

                val categoryListJson = categoryList?.optJSONArray("category_list")

                if (categoryListJson != null) {
                    var categories = mutableListOf<Category>()
                    for (i in 0 until categoryListJson.length()) {
                        val categoryJson = categoryListJson[i] as JSONObject
                        var parentId: Long? = categoryJson.optLong("parent_id", -1L)
                        if (parentId == -1L) {
                            parentId = null
                        }
                        categories.add(Category(
                            id = categoryJson.optLong("id"),
                            title = categoryJson.optString("title"),
                            lang = categoryJson.optInt("lang"),
                            parentId = parentId,
                            photo = categoryJson.optString("photo"),
                            responses = categoryJson.optJSONArrayAsList("responses")
                        ))
                    }

                    categories = categories.sortedBy { it.id }.toMutableList()

                    categories.forEachIndexed { index, category ->
                        logD("category: $category")

                        if (category.parentId == null) {
                            if (palette.isNotEmpty()) {
                                category.color = palette[index]
                            }
                            category.home = true
                            messages.add(Message(Message.Type.CATEGORY, category))

                            val userDashboard = JSONObject()
                            userDashboard.put("action", "get_category_list")
                            userDashboard.put("parent_id", category.id)
                            socket?.emit("user_dashboard", userDashboard)
                        } else {
                            if (category.parentId == activeCategoryChild?.id && activeCategoryChild?.children?.any { it.id == category.id } == false) {
                                activeCategoryChild?.children?.add(category)
                            }

//                            messages.forEach { message ->
//                                if (message.category?.id == category.parentId && message.category?.children?.contains(category) == false) {
//                                    message.category?.children?.add(category)
//                                } else {
////                                    message.category?.sections?.forEach { section ->
////                                        if (section.id == category.parentId) {
////                                            section.sections.add(Section.from(category))
////                                        }
////                                    }
//                                }
//                            }

                            if (activeCategoryChild == null) {
                                messages.forEach { message ->
                                    if (message.category?.id == category.parentId) {
                                        message.category?.children?.add(category)
                                    }
                                }
                            } else {
//                                val message = Message(Message.Type.CROSS_CHILDREN, category)
//                                    if (!messages.contains(message)) {
//                                    messages.add(message)
//                                }
                            }
                        }
                    }

                    if (!messages.isNullOrEmpty() && messages.all { !it.category?.children.isNullOrEmpty() } && activeCategoryChild == null) {
                        runOnUiThread {
                            feedbackView?.visibility = View.GONE
                            videoCallView?.visibility = View.GONE
                            recyclerView?.visibility = View.VISIBLE
                            footerView?.visibility = View.VISIBLE

                            chatAdapter.setNewMessages(messages)
                            scrollToTop()
                        }
                    } else {
                        runOnUiThread {
                            val activeCategoryChildMessage = Message(Message.Type.CROSS_CHILDREN, activeCategoryChild)

                            chatAdapter.setNewMessages(listOf(activeCategoryChildMessage))
                            scrollToTop()
                        }
                    }
                }
            }

        }?.on("form_init") { args ->

            logD("event [FORM_INIT]: $args")

            if (args.size == 1) {
                val formInitJson = args[0] as? JSONObject?
                logD("formInitJson: $formInitJson")
            }

        }?.on("form_final") { args ->

            logD("event [FORM_FINAL]: $args")

            if (args.size == 1) {
                val formFinalJson = args[0] as? JSONObject?
                logD("formFinalJson: $formFinalJson")
            }

        }?.on("send_configs") { args ->

            logD("event [SEND_CONFIGS]: $args")

            if (args.size == 1) {
                val configsJson = args[0] as? JSONObject?
                logD("configsJson: $configsJson")
            }

        }?.on("operator_greet") { args ->

            logD("event [OPERATOR_GREET]: $args")
            if (args.size == 1) {
                val operatorGreetJson = args[0] as? JSONObject?
                logD("JSONObject operatorGreetJson: $operatorGreetJson")

                if (operatorGreetJson != null) {
//                    val name = operatorGreet.optString("name")
                    val fullName = operatorGreetJson.optString("full_name")
                    val photo = operatorGreetJson.optString("photo")
                    var text = operatorGreetJson.optString("text")

                    val photoUrl = UrlUtil.getStaticUrl(photo)

                    logD("photoUrl: $photoUrl")

                    runOnUiThread {
                        bindOpponentData(Configs(
                            opponent = Configs.Opponent(
                                name = fullName,
                                secondName = getString(R.string.kenes_call_agent),
                                avatarUrl = photoUrl
                            )
                        ))

                        text = text.replace("{}", fullName)
                        chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text))
                        scrollToBottom()
                    }
                }
            }

        }?.on("operator_typing") { args ->

            logD("event [OPERATOR_TYPING]: $args")
            if (args.size == 1) {
                val operatorTypingJson = args[0] as? JSONObject?
                logD("JSONObject operatorTypingJson: $operatorTypingJson")
            }

        }?.on("feedback") { args ->

            logD("event [FEEDBACK]: $args")
            if (args.size == 1) {
                val feedbackJson = args[0] as? JSONObject?
                logD("JSONObject feedbackJson: $feedbackJson")

                if (feedbackJson != null) {
                    val buttonsJson = feedbackJson.optJSONArray("buttons")

                    val text = feedbackJson.optString("text")
//                    val chatId = feedback.optLong("chat_id")

                    if (buttonsJson != null) {
                        val ratingButtons = mutableListOf<RatingButton>()
                        for (i in 0 until buttonsJson.length()) {
                            val button = buttonsJson[i] as JSONObject
                            ratingButtons.add(RatingButton(
                                button.optString("title"),
                                button.optString("payload")
                            ))
                        }

                        runOnUiThread {
                            videoCallView?.visibility = View.GONE
                            recyclerView?.visibility = View.GONE
                            feedbackView?.visibility = View.VISIBLE
                            inputView?.let { hideKeyboard(it) }
                            footerView?.visibility = View.GONE

                            titleView?.text = text

                            var selectedRatingButton: RatingButton? = null
                            val ratingAdapter = RatingAdapter(ratingButtons) {
                                selectedRatingButton = it

                                if (selectedRatingButton != null) {
                                    rateButton?.isEnabled = true
                                }
                            }
                            ratingView?.adapter = ratingAdapter
                            ratingAdapter.notifyDataSetChanged()

                            rateButton?.setOnClickListener {
                                val userFeedback = JSONObject()
                                userFeedback.put("r", selectedRatingButton?.rating)
                                userFeedback.put("chat_id", selectedRatingButton?.chatId)
                                socket?.emit("user_feedback", userFeedback)

                                selectedRatingButton = null

                                rateButton?.isEnabled = false

                                ratingView?.adapter = null

                                videoCallView?.visibility = View.GONE
                                feedbackView?.visibility = View.GONE
                                recyclerView?.visibility = View.VISIBLE
                                footerView?.visibility = View.VISIBLE

                                bindOpponentData(this.configs)
                            }
                        }
                    }
                }
            }

        }?.on("message") { args ->
            logD("event [MESSAGE]: $args")

            if (args.size == 1) {
                val message = args[0] as? JSONObject?
                logD("message: $message")

//                [START] MESSAGE HANDLER FOR rtc.vlx.kz
//                val rtc = message?.optJSONObject("rtc")
//                val type = rtc?.optString("type")
//
//                if (type == "start") {
////                    sendOffer()
//
//                    val message2 = JSONObject()
//                    val rtc2 = JSONObject()
//                    rtc2.put("type", "prepare")
//                    message2.put("rtc", rtc2)
//                    socket?.emit("message", message2)
//                }
//
//                if (type == "prepare") {
//                    videoCallInitialize()
//
//                    runOnUiThread {
//                        videoCallButton?.isEnabled = false
//                        videoCallInfoView?.text = null
//                        videoCallView?.visibility = View.GONE
//                        recyclerView?.visibility = View.VISIBLE
//
//                        videoDialogView?.visibility = View.VISIBLE
//                    }
//
//                    val message2 = JSONObject()
//                    val rtc2 = JSONObject()
//                    rtc2.put("type", "ready")
//                    message2.put("rtc", rtc2)
//                    socket?.emit("message", message2)
//                }
//
//                if (type == "ready") {
//                    videoCallInitialize()
//
//                    runOnUiThread {
//                        videoCallButton?.isEnabled = false
//                        videoCallInfoView?.text = null
//                        videoCallView?.visibility = View.GONE
//                        recyclerView?.visibility = View.VISIBLE
//
//                        videoDialogView?.visibility = View.VISIBLE
//                    }
//
//                    sendOffer()
//                }
//
//                if (type == "answer") {
//                    peerConnection?.setRemoteDescription(
//                        SimpleSdpObserver(),
//                        SessionDescription(SessionDescription.Type.ANSWER, rtc.optString("sdp"))
//                    )
//                }
//
//                if (type == "candidate") {
//                    peerConnection?.addIceCandidate(IceCandidate(
//                        rtc.getString("id"),
//                        rtc.getInt("label"),
//                        rtc.getString("candidate")
//                    ))
//                }
//
//                if (type == "offer") {
//                    peerConnection?.setRemoteDescription(
//                        SimpleSdpObserver(),
//                        SessionDescription(SessionDescription.Type.OFFER, rtc.getString("sdp"))
//                    )
//
//                    sendAnswer()
//
//                    runOnUiThread {
//                        videoCallButton?.isEnabled = false
//                        videoCallInfoView?.text = null
//                        videoCallView?.visibility = View.GONE
//                        recyclerView?.visibility = View.VISIBLE
//
////                        videoCallInitialize()
//
//                        videoDialogView?.visibility = View.VISIBLE
//                    }
//                }
//
//                if (type == "hangup") {
//                    isInitiator = false
//
//                    closeVideoCall()
//
//                    runOnUiThread {
//                        videoDialogView?.visibility = View.GONE
//                        recyclerView?.visibility = View.GONE
//                        videoCallButton?.isEnabled = true
//                        videoCallInfoView?.text = null
//                        videoCallInfoView?.visibility = View.GONE
//                        videoCallView?.visibility = View.VISIBLE
//                    }
//                }
//                [END] MESSAGE HANDLER FOR rtc.vlx.kz

                if (message != null) {
                    val type = message.optString("type")
                    val text = message.getNullableString("text")
                    val noOnline = message.optBoolean("no_online")
                    val id = message.optString("id")
                    val action = message.getNullableString("action")
                    val time = message.optLong("time")
                    val sender = message.getNullableString("sender")

                    if (noOnline) {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("Внимание")
                                .setMessage(text)
                                .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
//                                    closeVideoCall()

                                    bindOpponentData(this.configs)

                                    recyclerView?.visibility = View.GONE
                                    videoCallInfoView?.text = null
                                    videoCallInfoView?.visibility = View.GONE
                                    videoCallButton?.isEnabled = true
                                    videoCallView?.visibility = View.VISIBLE

                                    dialog.dismiss()
                                }
                                .show()
                        }
                    } else if (!action.isNullOrBlank() && action == "operator_disconnect") {
                        runOnUiThread {
                            activeDialog = null

                            videoCallInfoView?.text = null
                            videoCallButton?.isEnabled = true

                            feedbackView?.visibility = View.GONE
                            videoCallView?.visibility = View.GONE
                            recyclerView?.visibility = View.VISIBLE

                            chatAdapter.addNewMessage(Message(Message.Type.NOTIFICATION, text, time))
                            scrollToBottom()

//                            chatAdapter.clearItems()
//                            recyclerView?.visibility = View.GONE
//                            callView?.visibility = View.VISIBLE
//                            callButton?.isEnabled = true
//                            infoView?.text = null
//                            infoView?.visibility = View.GONE
                        }
                    } else {
                        if (activeDialog == null) {
                            logD("text: $text")

//                                if (type == "offer") {
//                                    logD("connectToSignallingServer: OFFER")
//                                } else if (type == "accept") {
//                                    logD("connectToSignallingServer: ACCEPT:")
//                                    val rtc = message.optJSONObject("rtc")
//                                    logD("connectToSignallingServer: RTC: $rtc")
//                                }

                            runOnUiThread {
                                if (activeCategoryChild != null) {
                                    chatAdapter.setNewMessages(listOf(Message(Message.Type.RESPONSE, text, time, activeCategoryChild)))
                                    scrollToBottom()
                                } else {
                                    chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                                    scrollToBottom()
                                }
                            }
                        } else {
                            if (!sender.isNullOrBlank() && !activeDialog?.operatorId.isNullOrBlank() && sender == activeDialog?.operatorId) {
                                if (!id.isNullOrBlank() && !action.isNullOrBlank()) {
                                    runOnUiThread {
                                        activeDialog = null

                                        videoCallButton?.isEnabled = false
                                        videoCallInfoView?.text = text
                                        videoCallInfoView?.visibility = View.VISIBLE

                                        chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                                        scrollToBottom()
                                    }
                                }
                            } else {
                                Log.w(TAG, "WTF? Sender and call agent ids are DIFFERENT! sender: $sender, id: ${activeDialog?.operatorId}")
                            }
                        }
                    }
                }
            }

        }?.on(Socket.EVENT_DISCONNECT) {

            logD("event [EVENT_DISCONNECT]")

            runOnUiThread {
                isInitiator = false

                activeDialog = null

                idView.text = null

                bindOpponentData(this.configs)

                recyclerView?.visibility = View.GONE
                videoCallInfoView?.text = null
                videoCallInfoView?.visibility = View.GONE
                videoCallButton?.isEnabled = true
                videoCallView?.visibility = View.VISIBLE
            }

        }

        socket?.connect()
    }

    private fun sendAnswer() {
        val sdpMediaConstraints = MediaConstraints()

        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                logD("onCreateSuccess: " + sessionDescription.description)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)

//                [START] Sending answer with SDP for rtc.vlx.kz
                val message = JSONObject()
                val rtc = JSONObject()
                rtc.put("type", "answer")
                rtc.put("sdp", sessionDescription.description)
                message.put("rtc", rtc)
                socket?.emit("message", message)
//                [END] Sending answer with SDP for rtc.vlx.kz
            }
        }, sdpMediaConstraints)
    }

    private fun sendOffer() {
        val sdpMediaConstraints = MediaConstraints()

//        sdpMediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        sdpMediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                logD("onCreateSuccess: " + sessionDescription.description)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)

//                [START] Sending offer with SDP for kenes.vlx.kz
//                val message = JSONObject()
//                message.put("type", "offer")
//                message.put("sdp", sessionDescription.description)
//                socket?.emit("message", message)
//                [END] Sending offer with SDP for kenes.vlx.kz

//                [START] Sending offer with SDP for rtc.vlx.kz
                val message = JSONObject()
                val rtc = JSONObject()
                rtc.put("type", "offer")
                rtc.put("sdp", sessionDescription.description)
                message.put("rtc", rtc)
                socket?.emit("message", message)
//                [END] Sending offer with SDP for kenes.vlx.kz

                runOnUiThread {
                    chatAdapter.clearMessages()
                }
            }

            override fun onCreateFailure(s: String) {
                super.onCreateFailure(s)
                logD("onCreateFailure: $s")
            }
        }, sdpMediaConstraints)
    }

    private fun createPeerConnection(factory: PeerConnectionFactory): PeerConnection? {
        val iceServers = ArrayList<IceServer>()
        if (!this.iceServers.isNullOrEmpty()) {
            this.iceServers.forEach {
                iceServers.add(IceServer(it.url, it.username, it.credential))
            }
        } else {
//            iceServers.add(IceServer("stun:stun.l.google.com:19302"))
            iceServers.add(IceServer("stun:global.stun.twilio.com:3478?transport=udp"))
            iceServers.add(IceServer("turn:195.12.123.27:3478?transport=udp", "test", "test"))
        }
        val rtcConfig = RTCConfiguration(iceServers)
        rtcConfig.iceTransportsType = IceTransportsType.RELAY
        val peerConnectionConstraints = MediaConstraints()
        val peerConnectionObserver = object : Observer {
            override fun onSignalingChange(signalingState: SignalingState) {
                logD("onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                logD("onIceConnectionChange: $iceConnectionState")

                when (iceConnectionState) {
                    IceConnectionState.CLOSED, IceConnectionState.DISCONNECTED, IceConnectionState.FAILED -> {
                        runOnUiThread {
                            recyclerView?.visibility = View.GONE
                            videoDialogView?.visibility = View.GONE
                            videoCallButton?.isEnabled = true
                            videoCallInfoView?.text = null
                            videoCallInfoView?.visibility = View.GONE
                            videoCallView?.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                    }
                }
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                logD("onIceConnectionReceivingChange: $b")
            }

            override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
                logD("onIceGatheringChange: $iceGatheringState")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                logD("onIceCandidate: " + iceCandidate.sdp)

//                [START] Sending ICE Candidate for kenes.vlx.kz
//                val message = JSONObject()
//                message.put("type", "candidate")
//                message.put("label", iceCandidate.sdpMLineIndex)
//                message.put("id", iceCandidate.sdpMid)
//                message.put("candidate", iceCandidate.sdp)
//                logD("onIceCandidate: sending candidate $message")
//                socket?.emit("message", message)
//                [START] Sending ICE Candidate for kenes.vlx.kz

//                [START] Sending offer with SDP for rtc.vlx.kz
                val message = JSONObject()
                val rtc = JSONObject()
                rtc.put("type", "candidate")
                rtc.put("id", iceCandidate.sdpMid)
                rtc.put("label", iceCandidate.sdpMLineIndex)
                rtc.put("candidate", iceCandidate.sdp)
                message.put("rtc", rtc)
                socket?.emit("message", message)
//                [START] Sending offer with SDP for rtc.vlx.kz
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                logD("onIceCandidatesRemoved: " + iceCandidates.contentToString())
            }

            override fun onAddStream(mediaStream: MediaStream) {
                logD("onAddStream: " + mediaStream.videoTracks.size)

//                val remoteAudioTrack = mediaStream.audioTracks[0]
//                remoteAudioTrack.setEnabled(true);

                if (mediaStream.audioTracks.isNotEmpty()) {
                    remoteAudioTrack = mediaStream.audioTracks[0]
                    remoteAudioTrack?.setEnabled(true)
                }

                if (mediaStream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = mediaStream.videoTracks[0]
                    remoteVideoTrack?.setEnabled(true)
                    remoteVideoTrack?.addRenderer(VideoRenderer(remoteSurfaceView))
                }
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                logD("onRemoveStream: $mediaStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                logD("onDataChannel: $dataChannel")
            }

            override fun onRenegotiationNeeded() {
                logD("onRenegotiationNeeded")
                if (isInitiator) {
                    sendOffer()
                } else {
                    sendAnswer()
                }
            }
        }
        return factory.createPeerConnection(rtcConfig, peerConnectionConstraints, peerConnectionObserver)
    }

    private fun sendUserTextMessage(text: String) {
        val userMessage = JSONObject()
        try {
            userMessage.put("text", text)
        } catch (e: JSONException) {
            e.printStackTrace()
            logD("sendUserTextMessage: $e")
        }
        socket?.emit("user_message", userMessage)
    }

    private fun scrollToTop() {
        recyclerView?.scrollToPosition(0)
    }

    private fun scrollToBottom() {
        recyclerView?.let {
            val adapter = it.adapter
            if (adapter != null) {
//                logD("scrollTo: " + (adapter.itemCount - 1))
                it.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun updateActiveNavButtonTintColor() {
        if (activeNavButtonIndex >= 0 && activeNavButtonIndex < navButtons.size) {
            navButtons.forEach {
                setInactiveNavButtonTintColor(it)
            }
            setActiveNavButtonTintColor(navButtons[activeNavButtonIndex])
        }
    }

    private fun setActiveNavButtonTintColor(appCompatImageButton: AppCompatImageButton?) {
        appCompatImageButton?.setColorFilter(ContextCompat.getColor(this, R.color.kenes_blue))
    }

    private fun setInactiveNavButtonTintColor(appCompatImageButton: AppCompatImageButton?) {
        appCompatImageButton?.setColorFilter(ContextCompat.getColor(this, R.color.kenes_gray))
    }

    private fun closeVideoCall() {
        peerConnection?.dispose()
        peerConnection = null

        logD("Stopping capture.")
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        videoCapturer?.dispose()
        videoCapturer = null

//        logD("Closing video source.")
//        videoSource?.dispose()
        videoSource = null

//        localMediaStream?.dispose()
        localMediaStream = null

        logD("Closing peer connection factory.")
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        logD("Closing peer connection done.")

//        PeerConnectionFactory.stopInternalTracingCapture()
//        PeerConnectionFactory.shutdownInternalTracer()

        rootEglBase?.release()
        rootEglBase = null

//        localVideoTrack?.dispose()
        localVideoTrack = null
//        remoteVideoTrack?.dispose()
        remoteVideoTrack = null

        localSurfaceView?.release()
        localSurfaceView = null

        remoteSurfaceView?.release()
        remoteSurfaceView = null
    }

    override fun onDestroy() {
        super.onDestroy()

        activeDialog = null
//        isFilled = false
//        isCategoriesShown = false
        messages.clear()

        closeVideoCall()

        socket?.disconnect()
        socket = null

        opponentAvatarView = null
        opponentNameView = null
        opponentSecondNameView = null

        videoCallView = null
        videoCallButton = null
        videoCallInfoView = null

        footerView = null
        inputView = null
        attachmentButton = null

        chatAdapter.clearMessages()
        recyclerView?.adapter = null
        recyclerView = null
    }

    private fun logD(message: String) {
//        Log.d(TAG, message)
        if (message.length > 4000) {
            Log.d(TAG, message.substring(0, 4000))
            logD(message.substring(4000))
        } else {
            Log.d(TAG, message)
        }
    }

    private fun treeByChildren(category: Category?): List<Category> {
        fun inner(children: MutableList<Category>): List<Category> {
            val categories = children.filter {
                it.id == category?.id
            }
            return if (categories.isEmpty()) {
                children.flatMap { inner(it.children) }
            } else {
                categories
            }
        }

        return messages.flatMap { message ->
            val children = message.category?.children
            return when {
                children.isNullOrEmpty() -> {
                    listOf()
                }
                message.category?.id == category?.id -> {
                    children
                }
                else -> {
                    inner(children)
                }
            }
        }
    }

    private fun treeByParent(category: Category?): List<Category> {
        fun inner(children: MutableList<Category>): List<Category> {
            val categories = children.filter {
                it.id == category?.parentId
            }
            return if (categories.isEmpty()) {
                children.flatMap { inner(it.children) }
            } else {
                categories
            }
//            return children.flatMap {
//                if (it.id == category?.parentId) {
//                    it.children
//                } else {
//                    inner(it.children)
//                }
//            }
        }

        return messages.flatMap { message ->
            val children = message.category?.children
            return when {
                children.isNullOrEmpty() -> {
                    listOf()
                }
                message.category?.id == category?.parentId -> {
                    children
                }
                else -> {
                    inner(children)
                }
            }
        }
    }

}