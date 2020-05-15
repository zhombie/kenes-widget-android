package q19.kenes_widget

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import q19.kenes_widget.adapter.ChatAdapter
import q19.kenes_widget.adapter.ChatAdapterItemDecoration
import q19.kenes_widget.model.*
import q19.kenes_widget.model.Message
import q19.kenes_widget.network.HttpRequestHandler
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.jsonObject
import q19.kenes_widget.util.JsonUtil.optJSONArrayAsList
import q19.kenes_widget.util.JsonUtil.parse
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.hideKeyboard
import q19.kenes_widget.util.locale.LocaleAwareCompatActivity
import q19.kenes_widget.views.*
import q19.kenes_widget.webrtc.SimpleSdpObserver

class KenesWidgetV2Activity : LocaleAwareCompatActivity() {

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
            return Intent(context, KenesWidgetV2Activity::class.java)
        }
    }

    private var palette = intArrayOf()

    /**
     * Opponent info view variables: [headerView]
     */
    private var headerView: HeaderView? = null

    /**
     * Video call screen view variables: [videoCallView]
     */
    private var videoCallView: VideoCallView? = null

    /**
     * Audio call screen view variables: [audioCallView]
     */
    private var audioCallView: AudioCallView? = null

    /**
     * Info screen view variables: [infoView]
     */
    private var infoView: InfoView? = null

    /**
     * Footer view variables: [footerView]
     */
    private var footerView: FooterView? = null

    /**
     * Chat view variables: [recyclerView]
     */
    private var recyclerView: RecyclerView? = null

    /**
     * User feedback after dialog view variables: [feedbackView]
     */
    private var feedbackView: FeedbackView? = null

    /**
     * Bottom navigation view variables: [bottomNavigationView]
     */
    private var bottomNavigationView: BottomNavigationView? = null

    /**
     * Video dialog view variables: [videoDialogView]
     */
    private var videoDialogView: VideoDialogView? = null

    /**
     * Audio dialog view variables: [audioDialogView]
     */
    private var audioDialogView: AudioDialogView? = null

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

    private var configs = Configs()

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

    private var viewState: ViewState = ViewState.ChatBot
        set(value) {
            field = value
            runOnUiThread {
                updateViewState(value)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_widget_v2)

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
         * Bind [R.id.headerView] view.
         * Header view for opponent info display.
         */
        headerView = findViewById(R.id.headerView)

        /**
         * Bind [R.id.videoCallView] view
         */
        videoCallView = findViewById(R.id.videoCallView)

        /**
         * Bind [R.id.audioCallView] view
         */
        audioCallView = findViewById(R.id.audioCallView)

        /**
         * Bind [R.id.audioCallView] view
         */
        infoView = findViewById(R.id.infoView)

        /**
         * Bind [R.id.footerView] view.
         * Footer view for messenger.
         */
        footerView = findViewById(R.id.footerView)

        /**
         * Bind [R.id.recyclerView] view.
         * View for chat.
         */
        recyclerView = findViewById(R.id.recyclerView)

        /**
         * Bind [R.id.feedbackView] view.
         * Big screen view for user feedback after dialogue with a call agent.
         */
        feedbackView = findViewById(R.id.feedbackView)

        /**
         * Bind [R.id.bottomNavigationView] view
         */
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        /**
         * Bind [R.id.videoDialogView] view
         */
        videoDialogView = findViewById(R.id.videoDialogView)

        /**
         * Bind [R.id.audioDialogView] view
         */
        audioDialogView = findViewById(R.id.audioDialogView)

        // ------------------------------------------------------------------------


        // --------------------- Default screen setups ----------------------------

        /**
         * Default active navigation button of [bottomNavigationView]
         */
        bottomNavigationView?.setHomeNavButtonActive()

        /**
         * Default states of views
         */
        activeDialog = null

        headerView?.setDefaultState()
        feedbackView?.setDefaultState()
        footerView?.setDefaultState()
        videoCallView?.setDefaultState()
        audioCallView?.setDefaultState()

        viewState = ViewState.ChatBot

        // ------------------------------------------------------------------------


        /**
         * Configuration of home bottom navigation button action listeners (click/touch)
         */
        bottomNavigationView?.callback = object : BottomNavigationView.Callback {
            override fun onHomeNavButtonClicked() {
                viewState = ViewState.ChatBot

                chatAdapter.clearMessages()
                scrollToTop()

                messages.clear()

                activeCategoryChild = null

                socket?.emit("user_dashboard", jsonObject {
                    put("action", "get_category_list")
                    put("parent_id", 0)
                })
            }

            override fun onVideoNavButtonClicked() {
                viewState = ViewState.VideoDialog(State.IDLE)
            }

            override fun onAudioNavButtonClicked() {
                viewState = ViewState.AudioDialog(State.IDLE)
            }

            override fun onInfoNavButtonClicked() {
                viewState = ViewState.Info
            }
        }

        /**
         * Configuration of other button action listeners (click/touch)
         */
        headerView?.callback = object : HeaderView.Callback {
            override fun onHangUpButtonClicked() {
                isInitiator = false

                sendMessage(userMessage { rtc { type = Rtc.Type.HANGUP } })

                closeLiveCall()

                socket?.close()

                setNewStateByPreviousState(State.USER_DISCONNECT)

                bottomNavigationView?.setHomeNavButtonActive()
                connectToSignallingServer()
            }
        }

        videoCallView?.setOnCallClickListener {
            isInitiator = true

            viewState = ViewState.VideoDialog(State.PENDING)

            socket?.emit("initialize", jsonObject { put("video", true) })
        }

        audioCallView?.setOnCallClickListener {
            isInitiator = true

            viewState = ViewState.AudioDialog(State.PENDING)

            socket?.emit("initialize", jsonObject { put("audio", true) })
        }

        footerView?.callback = object : FooterView.Callback {
            override fun onGoToActiveDialogButtonClicked() {
                setNewStateByPreviousState(State.SHOWN)
            }

            override fun onAttachmentButtonClicked() {
                AlertDialog.Builder(this@KenesWidgetV2Activity)
                    .setMessage("Не реализовано")
                    .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            override fun onInputViewFocusChangeListener(v: View, hasFocus: Boolean) {
//                if (hasFocus) scrollToBottom()
            }

            override fun onInputViewClicked() {
//                scrollToBottom()
            }

            override fun onSendMessageButtonClicked(message: String) {
                if (message.isNotBlank()) {
                    sendUserMessage(message, true)
                }
            }
        }

        footerView?.setOnInputViewFocusChangeListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val text = v?.text.toString()

                if (text.isBlank()) {
                    return@setOnInputViewFocusChangeListener false
                }

                activeCategoryChild = null

                if (chatAdapter.isAllMessagesAreCategory()) {
                    chatAdapter.clearMessages()
                }

                sendUserMessage(text, true)

                return@setOnInputViewFocusChangeListener true
            }
            return@setOnInputViewFocusChangeListener false
        }

        footerView?.setOnTextChangedListener { s, _, _, _ ->
            if (s.isNullOrBlank()) {
                footerView?.disableSendMessageButton()
            } else {
                footerView?.enableSendMessageButton()
            }
        }

        videoDialogView?.callback = object : VideoDialogView.Callback {
            override fun onGoToChatButtonClicked() {
                viewState = ViewState.VideoDialog(State.HIDDEN)
            }

            override fun onHangUpButtonClicked() {
                isInitiator = false

                sendMessage(userMessage { rtc { type = Rtc.Type.HANGUP } })

                closeLiveCall()

                socket?.close()

                viewState = ViewState.VideoDialog(State.USER_DISCONNECT)

                bottomNavigationView?.setHomeNavButtonActive()
                connectToSignallingServer()
            }

            override fun onSwitchSourceButtonClicked() {
            }

            override fun onRemoteFrameClicked() {
                if (videoDialogView?.isControlButtonsVisible() == true) {
                    videoDialogView?.hideControlButtons()
                } else {
                    videoDialogView?.showControlButtons()
                }
            }
        }

        audioDialogView?.callback = object : AudioDialogView.Callback {
            override fun onGoToChatButtonClicked() {
                viewState = ViewState.AudioDialog(State.HIDDEN)
            }

            override fun onHangUpButtonClicked() {
                isInitiator = false

                sendMessage(userMessage { rtc { type = Rtc.Type.HANGUP } })

                closeLiveCall()

                socket?.close()

                viewState = ViewState.AudioDialog(State.USER_DISCONNECT)

                bottomNavigationView?.setHomeNavButtonActive()
                connectToSignallingServer()
            }
        }

        infoView?.callback = object : InfoView.Callback {
            override fun onPhoneNumberClicked(phoneNumber: String) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                startActivity(intent)
            }

            override fun onSocialClicked(contact: Configs.Contact) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contact.url))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }

            override fun onLanguageChangeClicked(language: Language) {
                val languages = Language.AllLanguages

                AlertDialog.Builder(this@KenesWidgetV2Activity)
                    .setTitle(R.string.kenes_select_language_from_list)
                    .setSingleChoiceItems(languages.map { it.value }.toTypedArray(), -1) { dialog, which ->
                        val selected = languages[which]
                        socket?.emit("user_language", jsonObject { put("language", selected.key) })
                        updateLocale(selected.locale)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.kenes_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
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
                chatAdapter.setNewMessages(this@KenesWidgetV2Activity.messages)
                scrollToTop()
//                } else {
//                    chatAdapter.setNewMessages(previous)
//                    scrollToTop()
//                }
            }

            override fun onCategoryChildClicked(category: Category) {
                activeCategoryChild = category

                if (category.responses.isNotEmpty()) {
                    socket?.emit("user_dashboard", jsonObject {
                        put("action", "get_response")
                        put("id", category.responses.first())
                    })
                } else {
                    socket?.emit("user_dashboard", jsonObject {
                        put("action", "get_category_list")
                        put("parent_id", category.id)
                    })
                }
            }

            override fun onGoToHomeClicked() {
                activeCategoryChild = null

                chatAdapter.setNewMessages(this@KenesWidgetV2Activity.messages)
                scrollToTop()
            }

            override fun onUrlInTextClicked(url: String) {
                if (url.startsWith("#")) {
                    val text = url.removePrefix("#")
                    sendUserMessage(text, false)
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        })

        recyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = chatAdapter
        recyclerView?.addItemDecoration(ChatAdapterItemDecoration(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, REQUEST_CODE_PERMISSIONS)
        }

        start()
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

    private fun initializeCallConnection(isVideoCall: Boolean = true) {
        rootEglBase = EglBase.create()

        if (isVideoCall) {
            runOnUiThread {
                videoDialogView?.localSurfaceView?.init(rootEglBase?.eglBaseContext, null)
                videoDialogView?.localSurfaceView?.setEnableHardwareScaler(true)
                videoDialogView?.localSurfaceView?.setMirror(true)

                videoDialogView?.remoteSurfaceView?.init(rootEglBase?.eglBaseContext, null)
                videoDialogView?.remoteSurfaceView?.setEnableHardwareScaler(true)
                videoDialogView?.remoteSurfaceView?.setMirror(true)
            }
        }

        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true)
        peerConnectionFactory = PeerConnectionFactory(null)

        if (isVideoCall) {
            peerConnectionFactory?.setVideoHwAccelerationOptions(
                rootEglBase?.eglBaseContext,
                rootEglBase?.eglBaseContext
            )
        }

        peerConnection = peerConnectionFactory?.let { createPeerConnection(it) }

        localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")

        if (isVideoCall) {
            localMediaStream?.addTrack(createVideoTrack())
        }

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
                name = configs?.optString("default_operator"),
                secondName = configs?.optString("title"),
                avatarUrl = UrlUtil.getStaticUrl(configs?.optString("image"))
            )

            contacts?.keys()?.forEach { key ->
                val value = contacts[key]

                if (value is String) {
                    this.configs.contacts.add(Configs.Contact(key, value))
                } else if (value is JSONArray) {
                    this.configs.phones = value.parse()
                }
            }

            infoView?.setContacts(this.configs.contacts)
            infoView?.setPhones(this.configs.phones)
            infoView?.setLanguage(Language.DEFAULT)

            this.configs.workingHours = Configs.WorkingHours(
                configs?.optString("message_kk"),
                configs?.optString("message_ru")
            )

            headerView?.setOpponentInfo(this.configs.opponent)
        } catch (e: Exception) {
//            e.printStackTrace()
            logDebug("ERROR! $e")
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
                    logDebug("iceServer: $iceServer")
                }
            }
        } catch (e: Exception) {
//            e.printStackTrace()
            logDebug("ERROR! $e")
        }
    }

    private fun connectToSignallingServer() {
        val signallingServerUrl = UrlUtil.getSignallingServerUrl()
        if (signallingServerUrl.isNullOrBlank()) {
            throw NullPointerException("Signalling server url is null.")
        } else {
            socket = IO.socket(signallingServerUrl)
        }

        socket?.on(Socket.EVENT_CONNECT) { args ->
            
            logDebug("event [EVENT_CONNECT]: $args")

            socket?.emit("user_dashboard", jsonObject {
                put("action", "get_category_list")
                put("parent_id", 0)
            })

        }?.on("call") { args ->

            logDebug("event [CALL]: $args")

            if (args.size != 1) {
                return@on
            }

            val call = args[0] as? JSONObject? ?: return@on

            logDebug("JSONObject call: $call")

            val type = call.optString("type")
            val media = call.optString("media")

            if (type == "accept") {
                activeDialog = Dialog(
                    operatorId = call.optString("operator"),
                    instance = call.optString("instance"),
                    media = call.optString("media")
                )

                if (media == "audio") {
                    viewState = ViewState.AudioDialog(State.PREPARATION)
                } else if (media == "video") {
                    viewState = ViewState.VideoDialog(State.PREPARATION)
                }

                logDebug("viewState: $viewState")
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

//                    messages.forEach { message ->
//                        if (message.category?.id == category.parentId && message.category?.children?.contains(category) == false) {
//                            message.category?.children?.add(category)
//                        } else {
//                            message.category?.sections?.forEach { section ->
//                                if (section.id == category.parentId) {
//                                    section.sections.add(Section.from(category))
//                                }
//                            }
//                        }
//                    }

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

            logDebug("event [FORM_INIT]: $args")

            if (args.size != 1) {
                return@on
            }

            val formInitJson = args[0] as? JSONObject? ?: return@on
            logDebug("formInitJson: $formInitJson")

        }?.on("form_final") { args ->

            logDebug("event [FORM_FINAL]: $args")

            if (args.size != 1) {
                return@on
            }

            val formFinalJson = args[0] as? JSONObject? ?: return@on
            logDebug("formFinalJson: $formFinalJson")

        }?.on("send_configs") { args ->

            logDebug("event [SEND_CONFIGS]: $args")

            if (args.size != 1) {
                return@on
            }

            val configsJson = args[0] as? JSONObject? ?: return@on
            logDebug("configsJson: $configsJson")

        }?.on("operator_greet") { args ->
            logDebug("event [OPERATOR_GREET]: $args")

            if (args.size != 1) {
                return@on
            }

            val operatorGreetJson = args[0] as? JSONObject? ?: return@on

            logDebug("JSONObject operatorGreetJson: $operatorGreetJson")

//            val name = operatorGreet.optString("name")
            val fullName = operatorGreetJson.optString("full_name")
            val photo = operatorGreetJson.optString("photo")
            var text = operatorGreetJson.optString("text")

            val photoUrl = UrlUtil.getStaticUrl(photo)

            logDebug("photoUrl: $photoUrl")

            logDebug("viewState: $viewState")

            text = text.replace("{}", fullName)

            runOnUiThread {
                headerView?.setOpponentInfo(Configs.Opponent(
                    name = fullName,
                    secondName = getString(R.string.kenes_call_agent),
                    avatarUrl = photoUrl
                ))

                if (viewState is ViewState.AudioDialog) {
                    audioDialogView?.setAvatar(photoUrl)
                    audioDialogView?.setName(fullName)
                }

                chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text))
                scrollToBottom()
            }

        }?.on("operator_typing") { args ->

            logDebug("event [OPERATOR_TYPING]: $args")

            if (args.size != 1) {
                return@on
            }

            val operatorTypingJson = args[0] as? JSONObject? ?: return@on
            logDebug("JSONObject operatorTypingJson: $operatorTypingJson")

        }?.on("feedback") { args ->

            logDebug("event [FEEDBACK]: $args")

            if (args.size != 1) {
                return@on
            }

            val feedbackJson = args[0] as? JSONObject? ?: return@on

            logDebug("JSONObject feedbackJson: $feedbackJson")

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

                viewState = ViewState.CallFeedback

                runOnUiThread {
                    footerView?.getInputView()?.let { hideKeyboard(it) }

                    feedbackView?.setTitle(text)
                    feedbackView?.setRatingButtons(ratingButtons)
                    feedbackView?.setOnRateButtonClickListener { ratingButton ->
                        socket?.emit("user_feedback", jsonObject {
                            put("r", ratingButton.rating)
                            put("chat_id", ratingButton.chatId)
                        })

                        viewState = ViewState.ChatBot

                        headerView?.setOpponentInfo(this.configs.opponent)
                    }
                }
            }

        }?.on("message") { args ->
            logDebug("event [MESSAGE]: $args")

            if (args.size != 1) {
                logDebug("ERROR! Strange message args behaviour.")
                return@on
            }

            val message = args[0] as? JSONObject? ?: return@on

            logDebug("message: $message")

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
                            headerView?.setOpponentInfo(this.configs.opponent)

                            setNewStateByPreviousState(State.IDLE)

                            dialog.dismiss()
                        }
                        .setOnCancelListener {
                            setNewStateByPreviousState(State.IDLE)
                        }
                        .show()
                }

                return@on
            }

            if (action == "operator_disconnect") {
                activeDialog = null

                closeLiveCall()

                viewState = ViewState.VideoDialog(State.OPPONENT_DISCONNECT)

                runOnUiThread {
                    chatAdapter.addNewMessage(Message(Message.Type.NOTIFICATION, text, time))
                    scrollToBottom()
                }

                return@on
            }

            rtc?.let {
                when (rtc.getNullableString("type")) {
                    Rtc.Type.START?.value -> {
                        logDebug("viewState (Rtc.Type.START?.value): $viewState")

                        sendMessage(userMessage { rtc { type = Rtc.Type.PREPARE } })
                    }
                    Rtc.Type.PREPARE?.value -> {
                        logDebug("viewState (Rtc.Type.PREPARE?.value): $viewState")

                        if (viewState is ViewState.VideoDialog) {
                            viewState = ViewState.VideoDialog(State.PREPARATION)

                            initializeCallConnection(isVideoCall = true)

                            sendMessage(userMessage { rtc { type = Rtc.Type.READY } })
                        } else if (viewState is ViewState.AudioDialog) {
                            viewState = ViewState.AudioDialog(State.PREPARATION)

                            initializeCallConnection(isVideoCall = false)

                            sendMessage(userMessage { rtc { type = Rtc.Type.READY } })
                        }
                    }
                    Rtc.Type.READY?.value -> {
                        logDebug("viewState (Rtc.Type.READY?.value): $viewState")

                        if (viewState is ViewState.VideoDialog) {
                            viewState = ViewState.VideoDialog(State.LIVE)

                            initializeCallConnection(isVideoCall = true)

                            sendOffer()
                        } else if (viewState is ViewState.AudioDialog) {
                            viewState = ViewState.AudioDialog(State.LIVE)

                            initializeCallConnection(isVideoCall = false)

                            sendOffer()
                        }
                    }
                    Rtc.Type.ANSWER?.value -> {
                        logDebug("viewState (Rtc.Type.ANSWER?.value): $viewState")

                        peerConnection?.setRemoteDescription(
                            SimpleSdpObserver(),
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                rtc.getString("sdp")
                            )
                        )
                    }
                    Rtc.Type.CANDIDATE?.value -> {
                        logDebug("viewState (Rtc.Type.CANDIDATE?.value): $viewState")

                        peerConnection?.addIceCandidate(
                            IceCandidate(
                                rtc.getString("id"),
                                rtc.getInt("label"),
                                rtc.getString("candidate")
                            )
                        )
                    }
                    Rtc.Type.OFFER?.value -> {
                        logDebug("viewState (Rtc.Type.OFFER?.value): $viewState")

                        setNewStateByPreviousState(State.LIVE)

                        peerConnection?.setRemoteDescription(
                            SimpleSdpObserver(),
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                rtc.getString("sdp")
                            )
                        )

                        sendAnswer()
                    }
                    Rtc.Type.HANGUP?.value -> {
                        logDebug("viewState (Rtc.Type.HANGUP?.value): $viewState")

                        isInitiator = false
                        activeDialog = null

                        setNewStateByPreviousState(State.IDLE)

                        closeLiveCall()
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
                logDebug("text: $text")

                if (from == "operator" && sender.isNullOrBlank() && action.isNullOrBlank()) {
                    runOnUiThread {
                        if (viewState is ViewState.VideoDialog) {
                            videoCallView?.setDisabledState(text)

                            chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                            scrollToBottom()
                        } else if (viewState is ViewState.AudioDialog) {
                            audioCallView?.setDisabledState(text)

                            chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                            scrollToBottom()
                        }
                    }
                } else {
                    runOnUiThread {
                        if (activeCategoryChild != null) {
                            chatAdapter.setNewMessages(listOf(Message(Message.Type.RESPONSE, text, time, activeCategoryChild)))
                            scrollToBottom()
                        } else {
                            chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                            scrollToBottom()
                        }
                    }
                }
            }

        }?.on(Socket.EVENT_DISCONNECT) {
            logDebug("event [EVENT_DISCONNECT]")

            isInitiator = false

            activeDialog = null

            viewState = ViewState.ChatBot

            closeLiveCall()

            runOnUiThread {
                headerView?.setOpponentInfo(this.configs.opponent)
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
                logDebug("onCreateSuccess: " + sessionDescription.description)
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
                logDebug("onCreateSuccess: " + sessionDescription.description)

                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)

                sendMessage(userMessage {
                    rtc { type = Rtc.Type.OFFER; sdp = sessionDescription.description }
                })
            }

            override fun onCreateFailure(s: String) {
                super.onCreateFailure(s)
                logDebug("onCreateFailure: $s")
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
                logDebug("onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                logDebug("onIceConnectionChange: $iceConnectionState")

                when (iceConnectionState) {
                    IceConnectionState.CLOSED, IceConnectionState.FAILED -> {
                        setNewStateByPreviousState(State.IDLE)
                    }
                    else -> {
                    }
                }
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                logDebug("onIceConnectionReceivingChange: $b")
            }

            override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
                logDebug("onIceGatheringChange: $iceGatheringState")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                logDebug("onIceCandidate: " + iceCandidate.sdp)

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
                logDebug("onIceCandidatesRemoved: " + iceCandidates.contentToString())
            }

            override fun onAddStream(mediaStream: MediaStream) {
                logDebug("onAddStream -> mediaStream.audioTracks.size: " + mediaStream.audioTracks.size)
                logDebug("onAddStream -> mediaStream.videoTracks.size: " + mediaStream.videoTracks.size)

                if (mediaStream.audioTracks.isNotEmpty()) {
                    remoteAudioTrack = mediaStream.audioTracks[0]
                    remoteAudioTrack?.setEnabled(true)
                }

                if (viewState is ViewState.VideoDialog) {
                    if (mediaStream.videoTracks.isNotEmpty()) {
                        remoteVideoTrack = mediaStream.videoTracks[0]
                        remoteVideoTrack?.setEnabled(true)
                        remoteVideoTrack?.addRenderer(VideoRenderer(videoDialogView?.remoteSurfaceView))
                    }
                }
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                logDebug("onRemoveStream: $mediaStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                logDebug("onDataChannel: $dataChannel")
            }

            override fun onRenegotiationNeeded() {
                logDebug("onRenegotiationNeeded")
                if (isInitiator) {
                    sendOffer()
                } else {
                    sendAnswer()
                }
            }
        }
        return factory.createPeerConnection(rtcConfig, peerConnectionConstraints, peerConnectionObserver)
    }

    private fun sendMessage(message: JSONObject) {
        logDebug("sendMessage: $message")
        socket?.emit("message", message)
    }

    private fun sendUserMessage(message: String, isInputClearText: Boolean = true) {
        socket?.emit("user_message", jsonObject {
            put("text", message)
        })

        if (isInputClearText) {
            footerView?.clearInputViewText()
        }

        chatAdapter.addNewMessage(Message(Message.Type.SELF, message))
        scrollToBottom()
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

    private fun setNewStateByPreviousState(state: State): Boolean {
        return when (viewState) {
            is ViewState.VideoDialog -> {
                viewState = ViewState.VideoDialog(state)
                true
            }
            is ViewState.AudioDialog -> {
                viewState = ViewState.AudioDialog(state)
                true
            }
            else -> false
        }
    }

    private fun updateViewState(viewState: ViewState) {
        when (viewState) {
            is ViewState.VideoDialog -> {
                infoView?.visibility = View.GONE

                when (viewState.state) {
                    State.IDLE, State.USER_DISCONNECT -> {
                        headerView?.hideHangupButton()

                        audioCallView?.setDefaultState()
                        audioCallView?.visibility = View.GONE

                        audioDialogView?.setDefaultState()
                        audioDialogView?.visibility = View.GONE

                        videoDialogView?.setDefaultState()
                        videoDialogView?.visibility = View.GONE

                        recyclerView?.visibility = View.GONE

                        footerView?.setGoToActiveDialogButtonState(null)
                        footerView?.visibility = View.GONE

                        bottomNavigationView?.setNavButtonsEnabled()

                        videoCallView?.setDefaultState()
                        videoCallView?.visibility = View.VISIBLE
                    }
                    State.PENDING -> {
                        headerView?.hideHangupButton()

                        videoCallView?.setDisabledState()

                        chatAdapter.clearMessages()
                    }
                    State.PREPARATION -> {
                        headerView?.hideHangupButton()

                        videoCallView?.setDisabledState()
                        videoCallView?.visibility = View.GONE

                        feedbackView?.setDefaultState()
                        feedbackView?.visibility = View.GONE

                        recyclerView?.visibility = View.VISIBLE
                    }
                    State.LIVE -> {
                        headerView?.showHangupButton()

                        videoCallView?.setDisabledState()
                        videoCallView?.visibility = View.GONE

                        feedbackView?.setDefaultState()
                        feedbackView?.visibility = View.GONE

                        bottomNavigationView?.setNavButtonsDisabled()

                        recyclerView?.visibility = View.VISIBLE

                        footerView?.visibility = View.VISIBLE

                        videoDialogView?.visibility = View.VISIBLE
                    }
                    State.OPPONENT_DISCONNECT -> {
                        headerView?.hideHangupButton()

                        videoCallView?.setDefaultState()
                        videoCallView?.visibility = View.GONE

                        feedbackView?.visibility = View.GONE

                        footerView?.setGoToActiveDialogButtonState(null)

                        bottomNavigationView?.setNavButtonsEnabled()

                        recyclerView?.visibility = View.VISIBLE
                    }
                    State.HIDDEN -> {
                        headerView?.showHangupButton()

                        recyclerView?.visibility = View.VISIBLE
                        scrollToBottom()

                        footerView?.setGoToActiveDialogButtonState(R.string.kenes_return_to_video_call)

                        videoDialogView?.visibility = View.INVISIBLE
                    }
                    State.SHOWN -> {
                        headerView?.hideHangupButton()

                        footerView?.getInputView()?.let { hideKeyboard(it) }

                        footerView?.setGoToActiveDialogButtonState(null)

                        videoDialogView?.visibility = View.VISIBLE
                    }
                }
            }
            is ViewState.AudioDialog -> {
                infoView?.visibility = View.GONE

                when (viewState.state) {
                    State.IDLE, State.USER_DISCONNECT -> {
                        headerView?.hideHangupButton()

                        videoCallView?.setDefaultState()
                        videoCallView?.visibility = View.GONE

                        audioDialogView?.setDefaultState()
                        audioDialogView?.visibility = View.GONE

                        videoDialogView?.setDefaultState()
                        videoDialogView?.visibility = View.GONE

                        recyclerView?.visibility = View.GONE

                        footerView?.setGoToActiveDialogButtonState(null)
                        footerView?.visibility = View.GONE

                        bottomNavigationView?.setNavButtonsEnabled()

                        audioCallView?.setDefaultState()
                        audioCallView?.visibility = View.VISIBLE
                    }
                    State.PENDING -> {
                        headerView?.hideHangupButton()

                        audioCallView?.setDisabledState()

                        chatAdapter.clearMessages()
                    }
                    State.PREPARATION -> {
                        headerView?.hideHangupButton()

                        audioCallView?.setDisabledState()
                        audioCallView?.visibility = View.GONE

                        feedbackView?.setDefaultState()
                        feedbackView?.visibility = View.GONE

                        recyclerView?.visibility = View.VISIBLE
                    }
                    State.LIVE -> {
                        headerView?.showHangupButton()

                        audioCallView?.setDisabledState()
                        audioCallView?.visibility = View.GONE

                        feedbackView?.visibility = View.GONE

                        bottomNavigationView?.setNavButtonsDisabled()

                        recyclerView?.visibility = View.VISIBLE

                        footerView?.visibility = View.VISIBLE

                        audioDialogView?.visibility = View.VISIBLE
                    }
                    State.OPPONENT_DISCONNECT -> {
                        headerView?.hideHangupButton()

                        audioCallView?.setDefaultState()
                        audioCallView?.visibility = View.GONE

                        feedbackView?.visibility = View.GONE

                        footerView?.setGoToActiveDialogButtonState(null)

                        bottomNavigationView?.setNavButtonsEnabled()

                        recyclerView?.visibility = View.VISIBLE
                    }
                    State.HIDDEN -> {
                        headerView?.showHangupButton()

                        recyclerView?.visibility = View.VISIBLE
                        scrollToBottom()

                        footerView?.setGoToActiveDialogButtonState(R.string.kenes_return_to_audio_call)

                        audioDialogView?.visibility = View.INVISIBLE
                    }
                    State.SHOWN -> {
                        headerView?.hideHangupButton()

                        footerView?.getInputView()?.let { hideKeyboard(it) }
                        footerView?.setGoToActiveDialogButtonState(null)

                        audioDialogView?.visibility = View.VISIBLE
                    }
                }
            }
            ViewState.CallFeedback -> {
                headerView?.hideHangupButton()

                audioCallView?.setDisabledState()
                audioCallView?.visibility = View.GONE

                videoCallView?.setDisabledState()
                videoCallView?.visibility = View.GONE

                audioDialogView?.setDefaultState()
                audioDialogView?.visibility = View.GONE

                videoDialogView?.setDefaultState()
                videoDialogView?.visibility = View.GONE

                infoView?.visibility = View.GONE

                recyclerView?.visibility = View.GONE

                footerView?.setDefaultState()
                footerView?.visibility = View.GONE

                feedbackView?.visibility = View.VISIBLE
            }
            ViewState.ChatBot -> {
                headerView?.hideHangupButton()

                audioCallView?.setDefaultState()
                audioCallView?.visibility = View.GONE

                videoCallView?.setDefaultState()
                videoCallView?.visibility = View.GONE

                audioDialogView?.setDefaultState()
                audioDialogView?.visibility = View.GONE

                videoDialogView?.setDefaultState()
                videoDialogView?.visibility = View.GONE

                feedbackView?.setDefaultState()
                feedbackView?.visibility = View.GONE

                infoView?.visibility = View.GONE

                bottomNavigationView?.setNavButtonsEnabled()

                recyclerView?.visibility = View.VISIBLE

                footerView?.setDefaultState()
                footerView?.visibility = View.VISIBLE
            }
            ViewState.Info -> {
                headerView?.hideHangupButton()

                audioCallView?.setDefaultState()
                audioCallView?.visibility = View.GONE

                videoCallView?.setDefaultState()
                videoCallView?.visibility = View.GONE

                audioDialogView?.setDefaultState()
                audioDialogView?.visibility = View.GONE

                videoDialogView?.setDefaultState()
                videoDialogView?.visibility = View.GONE

                feedbackView?.setDefaultState()
                feedbackView?.visibility = View.GONE

                recyclerView?.visibility = View.GONE

                footerView?.setDefaultState()
                footerView?.visibility = View.GONE

                infoView?.visibility = View.VISIBLE
            }
        }
    }

    private fun closeLiveCall() {
        peerConnection?.dispose()
        peerConnection = null

        logDebug("Stopping capture.")
        try {
            localVideoCapturer?.stopCapture()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        localVideoCapturer?.dispose()
        localVideoCapturer = null

        logDebug("Closing video source.")
//        localVideoSource?.dispose()
        localVideoSource = null

//        localMediaStream?.dispose()
        localMediaStream = null

        logDebug("Closing peer connection factory.")
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        logDebug("Closing peer connection done.")

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
        viewState = ViewState.ChatBot

        messages.clear()

        closeLiveCall()

        socket?.disconnect()
        socket = null

        headerView = null

        videoCallView = null

        footerView?.setDefaultState()
        footerView = null

        chatAdapter.clearMessages()
        recyclerView?.adapter = null
        recyclerView = null
    }

    private fun logDebug(message: String) {
//        Log.d(TAG, message)
        if (message.length > 4000) {
            Log.d(TAG, message.substring(0, 4000))
            logDebug(message.substring(4000))
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