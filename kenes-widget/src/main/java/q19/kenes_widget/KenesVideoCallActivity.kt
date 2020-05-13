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
import q19.kenes_widget.util.JsonUtil.jsonObject
import q19.kenes_widget.util.JsonUtil.optJSONArrayAsList
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.hideKeyboard
import q19.kenes_widget.views.VideoCallView
import q19.kenes_widget.views.VideoDialogView
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
     * Video call screen view variables: [videoCallView]
     */
    private var videoCallView: VideoCallView? = null

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
     * Video dialog view variables: [videoDialogView]
     */
    private var videoDialogView: VideoDialogView? = null

    private var activeNavButtonIndex = 0

    private val navButtons
        get() = listOf(homeNavButton, videoNavButton, audioNavButton, infoNavButton)

    private lateinit var chatAdapter: ChatAdapter

    private var socket: Socket? = null
    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null

    private var iceServers: MutableList<WidgetIceServer> = mutableListOf()

    private var localAudioSource: AudioSource? = null
    private var localVideoSource: VideoSource? = null

    private var localMediaStream: MediaStream? = null

    private var localVideoCapturer: VideoCapturer? = null

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

    private var isInitiator = false

    private var viewState: ViewState = ViewState.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_video_call)

        // TODO: Remove later, exhaustive on PROD
        UrlUtil.setHostname("https://kenes.vlx.kz")

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
         * Bind [R.id.videoCallView] view
         */
        videoCallView = findViewById(R.id.videoCallView)

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
         * Bind [R.id.videoDialogView] view
         */
        videoDialogView = findViewById(R.id.videoDialogView)

        // ------------------------------------------------------------------------


        // --------------------- Default screen setups ----------------------------

        /**
         * Default active navigation button [homeNavButton]
         */
        when (activeNavButtonIndex) {
            0 -> setActiveNavButtonTintColor(homeNavButton)
            1 -> setActiveNavButtonTintColor(videoNavButton)
            2 -> setActiveNavButtonTintColor(audioNavButton)
            3 -> setActiveNavButtonTintColor(infoNavButton)
        }

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
         * Configuration of home bottom navigation button action listeners (click/touch)
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

                videoCallView?.setDefaultState()

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

            videoCallView?.setDefaultState()

            footerView?.visibility = View.GONE
            feedbackView?.visibility = View.GONE
            recyclerView?.visibility = View.GONE
            videoCallView?.visibility = View.VISIBLE
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

        /**
         * Configuration of other button action listeners (click/touch)
         */
        videoCallView?.setOnCallClickListener {
            isInitiator = true

            inputView?.text?.clear()
            chatAdapter.clearMessages()

            socket?.emit("initialize", jsonObject { put("video", true) })

            videoCallView?.setDisabledState()
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

        videoDialogView?.callback = object : VideoDialogView.Callback {
            override fun onGoToChatButtonClicked() {
                videoDialogView?.visibility = View.INVISIBLE
            }

            override fun onHangUpButtonClicked() {
                isInitiator = false

                sendMessage(userMessage { rtc { type = Rtc.Type.HANGUP } })

                closeVideoCall()

                socket?.close()

                videoDialogView?.visibility = View.GONE
                recyclerView?.visibility = View.GONE

                videoCallView?.setDefaultState(false)
                videoCallView?.visibility = View.VISIBLE

                activeNavButtonIndex = 0
                updateActiveNavButtonTintColor()
                connectToSignallingServer()
            }

            override fun onSwitchSourceButtonClicked() {
            }
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
            videoDialogView?.localSurfaceView?.init(rootEglBase?.eglBaseContext, null)
            videoDialogView?.localSurfaceView?.setEnableHardwareScaler(true)
            videoDialogView?.localSurfaceView?.setMirror(true)

            videoDialogView?.remoteSurfaceView?.init(rootEglBase?.eglBaseContext, null)
            videoDialogView?.remoteSurfaceView?.setEnableHardwareScaler(true)
            videoDialogView?.remoteSurfaceView?.setMirror(true)
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
        localVideoCapturer = createVideoCapturer()
        localVideoSource = peerConnectionFactory?.createVideoSource(localVideoCapturer)
        localVideoCapturer?.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS)

        localVideoTrack = peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack?.setEnabled(true)
        localVideoTrack?.addRenderer(VideoRenderer(videoDialogView?.localSurfaceView))

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

            socket?.emit("user_dashboard", jsonObject {
                put("action", "get_category_list")
                put("parent_id", 0)
            })

        }?.on("call") { args ->

            logD("event [CALL]: $args")

            if (args.size != 1) {
                return@on
            }

            val call = args[0] as? JSONObject? ?: return@on

            logD("JSONObject call: $call")

            val type = call.optString("type")

            if (type == "accept") {
                activeDialog = Dialog(
                    operatorId = call.optString("operator"),
                    instance = call.optString("instance"),
                    media = call.optString("media")
                )

                runOnUiThread {
                    videoCallView?.setDisabledState()
                    videoCallView?.visibility = View.GONE

                    feedbackView?.visibility = View.GONE

                    recyclerView?.visibility = View.VISIBLE
                }
            }

        }?.on("category_list") { args ->

            if (args.size != 1) {
                return@on
            }

            val categoryList = args[0] as? JSONObject? ?: return@on
//            logD("categoryListJson: $categoryList")

            val categoryListJson = categoryList.optJSONArray("category_list") ?: return@on

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
//            logD("category: $category")

                if (category.parentId == null) {
                    if (palette.isNotEmpty()) {
                        category.color = palette[index]
                    }
                    category.home = true
                    messages.add(Message(Message.Type.CATEGORY, category))

                    socket?.emit("user_dashboard", jsonObject {
                        put("action", "get_category_list")
                        put("parent_id", category.id)
                    })
                } else {
                    if (category.parentId == activeCategoryChild?.id && activeCategoryChild?.children?.any { it.id == category.id } == false) {
                        activeCategoryChild?.children?.add(category)
                    }

//                        messages.forEach { message ->
//                            if (message.category?.id == category.parentId && message.category?.children?.contains(category) == false) {
//                                message.category?.children?.add(category)
//                            } else {
//                                message.category?.sections?.forEach { section ->
//                                    if (section.id == category.parentId) {
//                                        section.sections.add(Section.from(category))
//                                    }
//                                }
//                            }
//                        }

                    if (activeCategoryChild == null) {
                        messages.forEach { message ->
                            if (message.category?.id == category.parentId) {
                                message.category?.children?.add(category)
                            }
                        }
                    } else {
//                        val message = Message(Message.Type.CROSS_CHILDREN, category)
//                            if (!messages.contains(message)) {
//                            messages.add(message)
//                        }
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
                val activeCategoryChildMessage = Message(Message.Type.CROSS_CHILDREN, activeCategoryChild)

                runOnUiThread {
                    chatAdapter.setNewMessages(listOf(activeCategoryChildMessage))
                    scrollToTop()
                }
            }

        }?.on("form_init") { args ->

            logD("event [FORM_INIT]: $args")

            if (args.size != 1) {
                return@on
            }

            val formInitJson = args[0] as? JSONObject? ?: return@on
            logD("formInitJson: $formInitJson")

        }?.on("form_final") { args ->

            logD("event [FORM_FINAL]: $args")

            if (args.size != 1) {
                return@on
            }

            val formFinalJson = args[0] as? JSONObject? ?: return@on
            logD("formFinalJson: $formFinalJson")

        }?.on("send_configs") { args ->

            logD("event [SEND_CONFIGS]: $args")

            if (args.size != 1) {
                return@on
            }

            val configsJson = args[0] as? JSONObject? ?: return@on
            logD("configsJson: $configsJson")

        }?.on("operator_greet") { args ->
            logD("event [OPERATOR_GREET]: $args")

            if (args.size != 1) {
                return@on
            }

            val operatorGreetJson = args[0] as? JSONObject? ?: return@on

            logD("JSONObject operatorGreetJson: $operatorGreetJson")

//            val name = operatorGreet.optString("name")
            val fullName = operatorGreetJson.optString("full_name")
            val photo = operatorGreetJson.optString("photo")
            var text = operatorGreetJson.optString("text")

            val photoUrl = UrlUtil.getStaticUrl(photo)

            logD("photoUrl: $photoUrl")

            text = text.replace("{}", fullName)

            runOnUiThread {
                bindOpponentData(Configs(
                    opponent = Configs.Opponent(
                        name = fullName,
                        secondName = getString(R.string.kenes_call_agent),
                        avatarUrl = photoUrl
                    )
                ))

                chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text))
                scrollToBottom()
            }

        }?.on("operator_typing") { args ->

            logD("event [OPERATOR_TYPING]: $args")

            if (args.size != 1) {
                return@on
            }

            val operatorTypingJson = args[0] as? JSONObject? ?: return@on
            logD("JSONObject operatorTypingJson: $operatorTypingJson")

        }?.on("feedback") { args ->

            logD("event [FEEDBACK]: $args")

            if (args.size != 1) {
                return@on
            }

            val feedbackJson = args[0] as? JSONObject? ?: return@on

            logD("JSONObject feedbackJson: $feedbackJson")

            val buttonsJson = feedbackJson.optJSONArray("buttons")

            val text = feedbackJson.optString("text")
//            val chatId = feedback.optLong("chat_id")

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
                    videoCallView?.setDisabledState()
                    videoCallView?.visibility = View.GONE

                    recyclerView?.visibility = View.GONE

                    inputView?.let { hideKeyboard(it) }

                    footerView?.visibility = View.GONE

                    feedbackView?.visibility = View.VISIBLE

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
                        socket?.emit("user_feedback", jsonObject {
                            put("r", selectedRatingButton?.rating)
                            put("chat_id", selectedRatingButton?.chatId)
                        })

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

        }?.on("message") { args ->
            logD("event [MESSAGE]: $args")

            if (args.size != 1) {
                logD("ERROR! Strange message args behaviour.")
                return@on
            }

            val message = args[0] as? JSONObject? ?: return@on

            logD("message: $message")

            val text = message.getNullableString("text")
            val noOnline = message.optBoolean("no_online")
            val noResults = message.optBoolean("no_results")
            val id = message.optString("id")
            val action = message.getNullableString("action")
            val time = message.optLong("time")
            val sender = message.getNullableString("sender")
            val from = message.getNullableString("from")
            val rtc = message.optJSONObject("rtc")

            if (noOnline) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Внимание")
                        .setMessage(text)
                        .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
//                            closeVideoCall()

                            bindOpponentData(this.configs)

                            recyclerView?.visibility = View.GONE

                            videoCallView?.setDefaultState()
                            videoCallView?.visibility = View.VISIBLE

                            dialog.dismiss()
                        }
                        .show()
                }

                return@on
            }

            if (action == "operator_disconnect") {
//                activeDialog = null

                closeVideoCall()

                runOnUiThread {
                    videoCallView?.setDefaultState()
                    videoCallView?.visibility = View.GONE

                    feedbackView?.visibility = View.GONE

                    recyclerView?.visibility = View.VISIBLE

                    chatAdapter.addNewMessage(Message(Message.Type.NOTIFICATION, text, time))
                    scrollToBottom()
                }

                return@on
            }

            rtc?.let {
                when (rtc.getNullableString("type")) {
                    Rtc.Type.START?.value -> {
                        sendMessage(userMessage { rtc { type = Rtc.Type.PREPARE } })
                    }
                    Rtc.Type.PREPARE?.value -> {
                        videoCallInitialize()

                        runOnUiThread {
                            videoCallView?.setDisabledState()
                            videoCallView?.visibility = View.GONE

                            recyclerView?.visibility = View.VISIBLE

                            videoDialogView?.visibility = View.VISIBLE
                        }

                        sendMessage(userMessage { rtc { type = Rtc.Type.READY } })
                    }
                    Rtc.Type.READY?.value -> {
                        videoCallInitialize()

                        runOnUiThread {
                            videoCallView?.setDisabledState()
                            videoCallView?.visibility = View.GONE

                            recyclerView?.visibility = View.VISIBLE

                            videoDialogView?.visibility = View.VISIBLE
                        }

                        sendOffer()
                    }
                    Rtc.Type.ANSWER?.value -> {
                        peerConnection?.setRemoteDescription(
                            SimpleSdpObserver(),
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                rtc.getString("sdp")
                            )
                        )
                    }
                    Rtc.Type.CANDIDATE?.value -> {
                        peerConnection?.addIceCandidate(
                            IceCandidate(
                                rtc.getString("id"),
                                rtc.getInt("label"),
                                rtc.getString("candidate")
                            )
                        )
                    }
                    Rtc.Type.OFFER?.value -> {
                        peerConnection?.setRemoteDescription(
                            SimpleSdpObserver(),
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                rtc.getString("sdp")
                            )
                        )

                        sendAnswer()

                        runOnUiThread {
                            videoCallView?.setDisabledState()
                            videoCallView?.visibility = View.GONE

                            recyclerView?.visibility = View.VISIBLE

                            videoDialogView?.visibility = View.VISIBLE
                        }
                    }
                    Rtc.Type.HANGUP?.value -> {
                        isInitiator = false
                        activeDialog = null

                        closeVideoCall()

                        runOnUiThread {
                            videoDialogView?.visibility = View.GONE

                            recyclerView?.visibility = View.GONE

                            videoCallView?.setDefaultState()
                            videoCallView?.visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        return@on
                    }
                }
                return@on
            }

            if (activeDialog != null) {
//                if (!sender.isNullOrBlank() && !activeDialog?.operatorId.isNullOrBlank() && sender == activeDialog?.operatorId) {
//                    if (!id.isNullOrBlank() && !action.isNullOrBlank()) {
//
//                    }
//                } else {
//                    Log.w(TAG, "WTF? Sender and call agent ids are DIFFERENT! sender: $sender, id: ${activeDialog?.operatorId}")
//                }
                runOnUiThread {
                    chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                    scrollToBottom()
                }
            } else {
                logD("text: $text")

                if (from == "operator" && sender.isNullOrBlank() && action.isNullOrBlank()) {
                    runOnUiThread {
                        videoCallView?.setDisabledState(text)

                        chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                        scrollToBottom()
                    }
                } else {
                    runOnUiThread {
                        if (activeCategoryChild != null) {
                            chatAdapter.setNewMessages(listOf(
                                Message(Message.Type.RESPONSE, text, time, activeCategoryChild)
                            ))
                            scrollToBottom()
                        } else {
                            chatAdapter.addNewMessage(
                                Message(Message.Type.OPPONENT, text, time)
                            )
                            scrollToBottom()
                        }
                    }
                }
            }

        }?.on(Socket.EVENT_DISCONNECT) {

            logD("event [EVENT_DISCONNECT]")

            isInitiator = false

            activeDialog = null

            runOnUiThread {
                bindOpponentData(this.configs)

                recyclerView?.visibility = View.GONE

                videoCallView?.setDefaultState()
                videoCallView?.visibility = View.VISIBLE
            }

        }

        socket?.connect()
    }

    private fun sendAnswer() {
        val mediaConstraints = MediaConstraints()

//        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                logD("onCreateSuccess: " + sessionDescription.description)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)

                sendMessage(userMessage {
                    rtc { type = Rtc.Type.ANSWER; sdp = sessionDescription.description }
                })
            }
        }, mediaConstraints)
    }

    private fun sendOffer() {
        val mediaConstraints = MediaConstraints()

//        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                logD("onCreateSuccess: " + sessionDescription.description)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)

                sendMessage(userMessage {
                    rtc { type = Rtc.Type.OFFER; sdp = sessionDescription.description }
                })

                runOnUiThread {
                    chatAdapter.clearMessages()
                }
            }

            override fun onCreateFailure(s: String) {
                super.onCreateFailure(s)
                logD("onCreateFailure: $s")
            }
        }, mediaConstraints)
    }

    private fun createPeerConnection(factory: PeerConnectionFactory): PeerConnection? {
        val iceServers = ArrayList<IceServer>()
        if (!this.iceServers.isNullOrEmpty()) {
            this.iceServers.forEach {
                iceServers.add(IceServer(it.url, it.username, it.credential))
            }
        } else {
            iceServers.add(IceServer("stun:stun.l.google.com:19302"))
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
                    IceConnectionState.CLOSED, IceConnectionState.FAILED -> {
                        runOnUiThread {
                            recyclerView?.visibility = View.GONE

                            videoDialogView?.visibility = View.GONE

                            videoCallView?.setDefaultState()
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

                sendMessage(userMessage {
                    rtc {
                        type = Rtc.Type.CANDIDATE
                        id = iceCandidate.sdpMid
                        label = iceCandidate.sdpMLineIndex
                        candidate = iceCandidate.sdp
                    }
                })
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                logD("onIceCandidatesRemoved: " + iceCandidates.contentToString())
            }

            override fun onAddStream(mediaStream: MediaStream) {
                logD("onAddStream: " + mediaStream.videoTracks.size)

                if (mediaStream.audioTracks.isNotEmpty()) {
                    remoteAudioTrack = mediaStream.audioTracks[0]
                    remoteAudioTrack?.setEnabled(true)
                }

                if (mediaStream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = mediaStream.videoTracks[0]
                    remoteVideoTrack?.setEnabled(true)
                    remoteVideoTrack?.addRenderer(VideoRenderer(videoDialogView?.remoteSurfaceView))
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

    private fun sendMessage(message: JSONObject) {
        logD("sendMessage: $message")
        socket?.emit("message", message)
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
            localVideoCapturer?.stopCapture()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        localVideoCapturer?.dispose()
        localVideoCapturer = null

        logD("Closing video source.")
//        localVideoSource?.dispose()
        localVideoSource = null

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

        videoDialogView?.release()
    }

    override fun onDestroy() {
        super.onDestroy()

        activeDialog = null
        messages.clear()

        closeVideoCall()

        socket?.disconnect()
        socket = null

        opponentAvatarView = null
        opponentNameView = null
        opponentSecondNameView = null

        videoCallView = null

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