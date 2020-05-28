package q19.kenes_widget

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EdgeEffect
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.MergeAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fondesa.kpermissions.*
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.request.PermissionRequest
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestParams
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import com.squareup.picasso.Picasso
import cz.msebera.android.httpclient.Header
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.kenes_activity_widget_v2.*
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import q19.kenes_widget.adapter.ChatAdapter
import q19.kenes_widget.adapter.ChatAdapterItemDecoration
import q19.kenes_widget.adapter.ChatFooterAdapter
import q19.kenes_widget.core.locale.LocalizationActivity
import q19.kenes_widget.model.*
import q19.kenes_widget.model.Message
import q19.kenes_widget.network.DownloadFileTask
import q19.kenes_widget.network.IceServersTask
import q19.kenes_widget.network.WidgetConfigsTask
import q19.kenes_widget.ui.components.*
import q19.kenes_widget.util.*
import q19.kenes_widget.util.FileUtil.getRootDirPath
import q19.kenes_widget.util.FileUtil.openFile
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.jsonObject
import q19.kenes_widget.webrtc.SimpleSdpObserver
import java.io.File
import java.io.FileNotFoundException

class KenesWidgetV2Activity : LocalizationActivity(), PermissionRequest.Listener {

    companion object {
        private const val TAG = "LOL"

        private const val VIDEO_RESOLUTION_WIDTH = 1024
        private const val VIDEO_RESOLUTION_HEIGHT = 768
        private const val FPS = 24

        private const val AUDIO_TRACK_ID = "ARDAMSa0"
        private const val VIDEO_TRACK_ID = "ARDAMSv0"

        private const val FILE_PICKER_REQUEST_CODE = 101

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, KenesWidgetV2Activity::class.java)
        }
    }

    private var palette = intArrayOf()

    private var rootView: FrameLayout? = null

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

    private var progressView: ProgressView? = null

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

    private var mergeAdapter: MergeAdapter? = null
    private var chatAdapter: ChatAdapter? = null
    private var chatFooterAdapter: ChatFooterAdapter? = null

    private var socket: Socket? = null
    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null

    private var localAudioSource: AudioSource? = null
    private var localVideoSource: VideoSource? = null

    private var localMediaStream: MediaStream? = null

    private var localVideoCapturer: VideoCapturer? = null

    private var localAudioTrack: AudioTrack? = null
    private var remoteAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var audioManager: AudioManager? = null

    private val permissionsRequest by lazy {
        permissionsBuilder(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).build()
    }

    private var configs = Configs()
    private var chatBot = ChatBot()
    private var dialog = Dialog()

    private var iceServers = listOf<WidgetIceServer>()

    private var viewState: ViewState = ViewState.ChatBot
        set(value) {
            field = value
            runOnUiThread {
                updateViewState(value)
            }
        }

    private var isUserPromptMode: Boolean = false
        set(value) {
            if (field == value) {
                return
            }

            field = value

            if (value) {
                runOnUiThread {
//                    chatAdapter?.clearMessages(false)
                    chatAdapter?.clearCategoryMessages()
                }

                chatBot.activeCategory = null
            }

            chatBot.isLocked = value
        }

    private var isLoading: Boolean = false
        set(value) {
            if (field == value) return
            if (dialog.isOnLive) return

            field = value

            runOnUiThread {
                if (value) {
                    recyclerView?.visibility = View.GONE
                    progressView?.show()
                } else {
                    progressView?.hide()
                    recyclerView?.visibility = View.VISIBLE
                }
            }
        }

    private var isSwitchToCallAgentClicked: Boolean = false

    private var chatRecyclerState: Parcelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_widget_v2)

        permissionsRequest.addListener(this)
        permissionsRequest.send()

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
        rootView = findViewById(R.id.rootView)

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

        progressView = findViewById(R.id.progressView)

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
        isLoading = true

        /**
         * Default states of views
         */
        dialog.clear()

        isUserPromptMode = false

        headerView?.hideHangupButton()
        headerView?.setOpponentInfo(Configs.Opponent(
            "Kenes",
            "Smart Bot",
            drawableRes = R.drawable.kenes_ic_robot
        ))

        feedbackView?.setDefaultState()
        footerView?.setDefaultState()
        videoCallView?.setDefaultState()
        audioCallView?.setDefaultState()

        viewState = ViewState.ChatBot

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        chatBot.callback = object : ChatBot.Callback {
            override fun onBasicCategoriesLoaded(categories: List<Category>) {
                val messages = categories
                    .sortedBy { it.id }
                    .mapIndexed { index, category ->
                        if (palette.isNotEmpty()) {
                            category.color = palette[index % palette.size]
                        }
                        Message(Message.Type.CATEGORY, category)
                    }

                runOnUiThread {
                    chatFooterAdapter?.clear()
                    chatAdapter?.setNewMessages(messages)
                }

                isLoading = false
            }
        }

        // ------------------------------------------------------------------------


        /**
         * Configuration of home bottom navigation button action listeners (click/touch)
         */
        bottomNavigationView?.callback = object : BottomNavigationView.Callback {
            override fun onHomeNavButtonClicked() {
                isUserPromptMode = false

                chatBot.clear()

                chatAdapter?.clear()
                chatFooterAdapter?.clear()

                sendUserDashboard(jsonObject {
                    put("action", "get_category_list")
                    put("parent_id", 0)
                })

                isLoading = true

                viewState = ViewState.ChatBot
            }

            override fun onVideoNavButtonClicked() {
                isUserPromptMode = false

                chatBot.clear()

                chatAdapter?.clear()
                chatFooterAdapter?.clear()

                viewState = ViewState.VideoDialog(State.IDLE)
            }

            override fun onAudioNavButtonClicked() {
                isUserPromptMode = false

                chatBot.clear()

                chatAdapter?.clear()
                chatFooterAdapter?.clear()

                viewState = ViewState.AudioDialog(State.IDLE)
            }

            override fun onInfoNavButtonClicked() {
                isUserPromptMode = false

                chatBot.clear()

                chatAdapter?.clear()
                chatFooterAdapter?.clear()

                viewState = ViewState.Info
            }
        }

        /**
         * Configuration of other button action listeners (click/touch)
         */
        headerView?.callback = object : HeaderView.Callback {
            override fun onHangupButtonClicked() {
                showHangupConfirmAlert { hangupLiveCall() }
            }
        }

        videoCallView?.setOnCallClickListener {
            val isPermissionRequestSent = checkPermissions()
            if (isPermissionRequestSent) {
                return@setOnCallClickListener
            } else {
                if (dialog.isInitiator) {
                    showAlreadyCallingAlert {
                        socket?.emit("user_disconnect")
                        dialog.isInitiator = false
                    }
                    return@setOnCallClickListener
                }

                dialog.isInitiator = true

                viewState = ViewState.VideoDialog(State.PENDING)

                socket?.emit("initialize", jsonObject { put("video", true) })
            }
        }

        audioCallView?.setOnCallClickListener {
            val isPermissionRequestSent = checkPermissions()
            if (isPermissionRequestSent) {
                return@setOnCallClickListener
            } else {
                if (dialog.isInitiator) {
                    showAlreadyCallingAlert {
                        socket?.emit("user_disconnect")
                        dialog.isInitiator = false
                    }
                    return@setOnCallClickListener
                }

                dialog.isInitiator = true

                viewState = ViewState.AudioDialog(State.PENDING)

                socket?.emit("initialize", jsonObject { put("audio", true) })
            }
        }

        rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val rec = Rect()
            rootView?.getWindowVisibleDisplayFrame(rec)

            // finding screen height
            val screenHeight = rootView?.rootView?.height ?: 0

            // finding keyboard height
            val keypadHeight = screenHeight - rec.bottom

            if (keypadHeight > screenHeight * 0.15) {
                bottomNavigationView?.hideButtons()
            } else {
                bottomNavigationView?.showButtons()
            }
        }

        footerView?.callback = object : FooterView.Callback {
            override fun onGoToActiveDialogButtonClicked() {
                setNewStateByPreviousState(State.SHOWN)
            }

            override fun onAttachmentButtonClicked() {
                MaterialFilePicker()
                    .withActivity(this@KenesWidgetV2Activity)
                    .withCloseMenu(true)
                    .withRequestCode(FILE_PICKER_REQUEST_CODE)
                    .start()
            }

            override fun onInputViewFocusChangeListener(v: View, hasFocus: Boolean) {
                if (!hasFocus) hideKeyboard(v)
            }

            override fun onInputViewClicked() {
//                scrollToBottom()
            }

            override fun onSendMessageButtonClicked(message: String) {
                if (message.isNotBlank()) {
                    isUserPromptMode = true
                    sendUserMessage(message, true)
                    chatAdapter?.notifyDataSetChanged()

                    isLoading = true
                }
            }
        }

        footerView?.setOnInputViewFocusChangeListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val text = v?.text.toString()

                if (text.isBlank()) {
                    return@setOnInputViewFocusChangeListener false
                }

                isUserPromptMode = true
                sendUserMessage(text, true)
                chatAdapter?.notifyDataSetChanged()

                isLoading = true

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

            override fun onHangupButtonClicked() {
                showHangupConfirmAlert { hangupLiveCall() }
            }

            override fun onSwitchSourceButtonClicked() {
                (localVideoCapturer as? CameraVideoCapturer?)?.switchCamera(null)
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

            override fun onHangupButtonClicked() {
                showHangupConfirmAlert { hangupLiveCall() }
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
                val items = languages.map { it.value }.toTypedArray()
                showLanguageSelectionAlert(items) { which ->
                    val selected = languages[which]

                    socket?.emit("user_language", jsonObject { put("language", selected.key) })

                    setLanguage(selected.locale)
                }
            }
        }

        setupRecyclerView()

        setKeyboardBehavior()

        fetchWidgetConfigs()
        fetchIceServers()

        connectToSignallingServer()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(object : ChatAdapter.Callback {
            override fun onReturnBackClicked(category: Category) {
                hideKeyboard()

//                logDebug("onReturnBackClicked: $category")

                val categories = chatBot.allCategories.filter { it.id == category.parentId }

//                logDebug("onReturnBackClicked: $categories")

                val messages = if (categories.all { it.parentId == null }) {
                    runOnUiThread {
                        chatFooterAdapter?.clear()
                    }

                    chatBot.basicCategories.map { Message(Message.Type.CATEGORY, it) }
                } else {
                    categories.map { Message(Message.Type.CROSS_CHILDREN, it) }
                }

                chatBot.activeCategory = null

                runOnUiThread {
                    chatAdapter?.setNewMessages(messages)
                }

                chatRecyclerState?.let { chatRecyclerState ->
                    recyclerView?.layoutManager?.onRestoreInstanceState(chatRecyclerState)
                }
            }

            override fun onCategoryChildClicked(category: Category) {
                hideKeyboard()

                chatBot.activeCategory = category

                chatRecyclerState = recyclerView?.layoutManager?.onSaveInstanceState()

                chatFooterAdapter?.showGoToHomeButton()

                if (category.responses.isNotEmpty()) {
                    sendUserDashboard(jsonObject {
                        put("action", "get_response")
                        put("id", category.responses.first())
                    })
                } else {
                    sendUserDashboard(jsonObject {
                        put("action", "get_category_list")
                        put("parent_id", category.id)
                    })
                }

                isLoading = true
            }

            override fun onUrlInTextClicked(url: String) {
                if (url.startsWith("#")) {
                    chatFooterAdapter?.showGoToHomeButton()

                    val text = url.removePrefix("#")
                    isUserPromptMode = true
                    sendUserMessage(text, false)
                    chatAdapter?.notifyDataSetChanged()

                    isLoading = true
                } else {
                    showOpenLinkConfirmAlert(url) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onImageClicked(imageView: ImageView, imageUrl: String) {
                imageView.showFullscreenImage(imageUrl)
            }

            override fun onImageClicked(imageView: ImageView, bitmap: Bitmap) {
                imageView.showFullscreenImage(bitmap)
            }

            override fun onImageLoadCompleted() {
            }

            override fun onFileClicked(media: Media) {
                val file = File(getRootDirPath() + "/" + media.name)
                if (file.exists()) {
                    openFile(file)
                } else {
                    DownloadFileTask(this@KenesWidgetV2Activity, media.name ?: "temp") {
                        openFile(it)
                    }.execute(media.fileUrl)
                }
            }
        })

        recyclerView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context)
                    .apply { color = ContextCompat.getColor(this@KenesWidgetV2Activity, R.color.kenes_light_blue) }
            }
        }
        recyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView?.isNestedScrollingEnabled = false

        chatFooterAdapter = ChatFooterAdapter()
        chatFooterAdapter?.callback = object : ChatFooterAdapter.Callback {
            override fun onGoToHomeClicked() {
                hideKeyboard()

                isUserPromptMode = false

                val messages = chatBot.basicCategories.map { Message(Message.Type.CATEGORY, it) }

                runOnUiThread {
                    chatFooterAdapter?.clear()
                    chatAdapter?.setNewMessages(messages)
                }

                chatRecyclerState?.let { chatRecyclerState ->
                    recyclerView?.layoutManager?.onRestoreInstanceState(chatRecyclerState)
                }
            }

            override fun onSwitchToCallAgentClicked() {
                if (dialog.isInitiator) {
                    showAlreadyCallingAlert {
                        socket?.emit("user_disconnect")
                        dialog.isInitiator = false
                    }
                    return
                }

                dialog.isInitiator = true

                isSwitchToCallAgentClicked = true

//                chatAdapter?.isActionButtonEnabled = false
//                chatAdapter?.isGoToHomeButtonEnabled = false

                footerView?.enableAttachmentButton()

                socket?.emit("initialize", jsonObject { put("video", false) })
            }

            override fun onRegisterAppealClicked() {
                logDebug("onRegisterAppealClicked")
            }
        }

        mergeAdapter = MergeAdapter(chatAdapter, chatFooterAdapter)

        recyclerView?.adapter = mergeAdapter

        recyclerView?.addItemDecoration(ChatAdapterItemDecoration(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setKeyboardBehavior() {
        recyclerView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                hideKeyboard()
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions(): Boolean {
        val permissions = permissionsRequest.checkStatus()
        if (permissions.anyShouldShowRationale() || permissions.anyDenied() || permissions.anyPermanentlyDenied()) {
            permissionsRequest.send()
            return true
        }
        return false
    }

    override fun onPermissionsResult(result: List<PermissionStatus>) {
        if (result.allGranted()) {
            return
        }
        showPermanentlyDeniedDialog(getString(R.string.kenes_permissions_necessity_info)) { isPositive ->
            if (isPositive) {
                if (result.anyPermanentlyDenied()) {
                    val intent = createAppSettingsIntent()
                    startActivity(intent)
                } else if (result.anyShouldShowRationale()) {
                    permissionsRequest.send()
                }
            } else {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val filePath = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
            logDebug("filePath: $filePath")
            
            if (filePath == null) {
                return
            }
            
            val client = AsyncHttpClient()

            val params = RequestParams()

            val type: String?

            try {
                val file = File(filePath)
                params.put("file", file)

                type = when {
                    filePath.endsWith("jpg") || filePath.endsWith("jpeg") || filePath.endsWith("png") ->
                        "image"
                    filePath.endsWith("mp3") || filePath.endsWith("wav") || filePath.endsWith("opus") || filePath.endsWith("ogg") ->
                        "audio"
                    filePath.endsWith("mp4") || filePath.endsWith("mov") || filePath.endsWith("webm") || filePath.endsWith("mkv") || filePath.endsWith("avi") ->
                        "video"
//                    filePath.endsWith("doc") || filePath.endsWith("docx") || filePath.endsWith("xls") || filePath.endsWith("xlsx") || filePath.endsWith("pdf") ->
//                        "document"
                    else ->
                        "file"
                }

                params.put("type", type)
            } catch (e: FileNotFoundException) {
                Log.e(TAG, e.message ?: "")
                return
            }

            client.post(UrlUtil.getHostname() + "/upload", params, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
                    super.onSuccess(statusCode, headers, response)

                    val hash = response?.optString("hash")
                    var url = response?.optString("url")

                    if (hash.isNullOrBlank() || url.isNullOrBlank()) {
                        return
                    }

                    socket?.emit("user_message", jsonObject { put(type, url) })

                    if (url.startsWith(UrlUtil.STATIC_PATH)) {
                        url = UrlUtil.getHostname() + url
                    }

                    val media = if (type == "image") {
                        Media(imageUrl = url, name = hash, ext = hash.split(".").last())
                    } else {
                        Media(fileUrl = url, name = hash, ext = hash.split(".").last())
                    }

                    runOnUiThread {
                        chatAdapter?.addNewMessage(Message(Message.Type.USER, media))
                        scrollToBottom()
                    }
                }
            })
        }
    }

    override fun onBackPressed() {
        showWidgetCloseConfirmDialog { finish() }
    }

    private fun initializeCallConnection(isVideoCall: Boolean = true) {
        rootEglBase = EglBase.create()

        if (isVideoCall) {
            runOnUiThread {
                videoDialogView?.localSurfaceView?.init(rootEglBase?.eglBaseContext, null)
                videoDialogView?.localSurfaceView?.setEnableHardwareScaler(true)
                videoDialogView?.localSurfaceView?.setMirror(false)

                videoDialogView?.remoteSurfaceView?.init(rootEglBase?.eglBaseContext, null)
                videoDialogView?.remoteSurfaceView?.setEnableHardwareScaler(true)
                videoDialogView?.remoteSurfaceView?.setMirror(true)
            }
        }

        PeerConnectionFactory.initializeAndroidGlobals(this, true)
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
        val task = WidgetConfigsTask(UrlUtil.getHostname() + "/configs")

        val data = task.run()

        data?.let {
            configs = data

            infoView?.setContacts(configs.contacts)
            infoView?.setPhones(configs.phones)
            infoView?.setLanguage(Language.from(getCurrentLanguage()))

            headerView?.setOpponentInfo(configs.opponent)
        }
    }

    private fun fetchIceServers() {
        val task = IceServersTask(UrlUtil.getHostname() + "/ice_servers")

        val data = task.run()

        data?.let {
            iceServers = data
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

            sendUserDashboard(jsonObject {
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
                dialog = Dialog(
                    operatorId = call.optString("operator"),
                    instance = call.optString("instance"),
                    media = call.optString("media")
                )

                runOnUiThread {
                    if (chatAdapter?.clearCategoryMessages() == true) {
                        chatAdapter?.notifyDataSetChanged()
                    }

                    footerView?.disableAttachmentButton()
                }

                if (media == "audio") {
                    viewState = ViewState.AudioDialog(State.PREPARATION)
                } else if (media == "video") {
                    viewState = ViewState.VideoDialog(State.PREPARATION)
                }

                logDebug("viewState: $viewState")
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

            if (isSwitchToCallAgentClicked) {
                dialog = Dialog(
                    operatorId = fullName,
                    media = "text"
                )

                runOnUiThread {
                    headerView?.showHangupButton()
                    footerView?.enableAttachmentButton()
                }
            }

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

                chatAdapter?.addNewMessage(Message(Message.Type.OPPONENT, text))
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

            closeLiveCall()

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

                        viewState = ViewState.VideoDialog(State.FINISHED)
                    }
                }
            }

        }?.on("user_queue") { args ->
            logDebug("event [USER_QUEUE]: $args")

            if (args.size != 1) {
                return@on
            }

            val userQueue = args[0] as? JSONObject? ?: return@on

            logDebug("userQueue: $userQueue")

            val count = userQueue.getInt("count")
//            val channel = userQueue.getInt("channel")

//            logDebug("userQueue -> count: $count")

            if (count > 1) {
                runOnUiThread {
                    if (viewState is ViewState.VideoDialog) {
                        videoCallView?.setPendingQueueCount(count)
                    } else if (viewState is ViewState.AudioDialog) {
                        audioCallView?.setPendingQueueCount(count)
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

            val text = message.getNullableString("text")?.trim()
            val noOnline = message.optBoolean("no_online")
            val noResults = message.optBoolean("no_results")
//            val id = message.optString("id")
            val action = message.getNullableString("action")
            val time = message.optLong("time")
            val sender = message.getNullableString("sender")
            val from = message.getNullableString("from")
            val media = message.optJSONObject("media")
            val rtc = message.optJSONObject("rtc")
            val fuzzyTask = message.optBoolean("fuzzy_task")

            if (noResults && from.isNullOrBlank() && sender.isNullOrBlank() && action.isNullOrBlank()) {
                runOnUiThread {
                    chatAdapter?.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                    chatFooterAdapter?.showSwitchToCallAgentButton()
                    scrollToBottom()
                }

                isLoading = false

                return@on
            }

            if (fuzzyTask) {
            }

            if (noOnline) {
                dialog.isInitiator = false

                runOnUiThread {
                    showNoOnlineCallAgents(text) {
                        isLoading = false
                        setNewStateByPreviousState(State.IDLE)
                    }
                }

                return@on
            }

            if (action == "operator_disconnect") {
                closeLiveCall()

                viewState = ViewState.VideoDialog(State.OPPONENT_DISCONNECT)

                runOnUiThread {
                    chatAdapter?.addNewMessage(Message(Message.Type.NOTIFICATION, text, time))
                    scrollToBottom()
                }

                return@on
            }

            if (chatBot.activeCategory != null) {
                val messages = listOf(Message(Message.Type.RESPONSE, text, time, chatBot.activeCategory))
                runOnUiThread {
                    chatFooterAdapter?.showGoToHomeButton()
                    chatAdapter?.setNewMessages(messages)
                }
                isLoading = false
                return@on
            }

            rtc?.let {
                when (rtc.getNullableString("type")) {
                    Rtc.Type.START?.value -> {
                        logDebug("viewState (Rtc.Type.START?.value): $viewState")

                        runOnUiThread {
                            headerView?.showHangupButton()
                            footerView?.visibility = View.VISIBLE
                        }

                        sendMessage(rtc { type = Rtc.Type.PREPARE })
                    }
                    Rtc.Type.PREPARE?.value -> {
                        logDebug("viewState (Rtc.Type.PREPARE?.value): $viewState")

                        if (viewState is ViewState.VideoDialog) {
                            viewState = ViewState.VideoDialog(State.PREPARATION)

                            initializeCallConnection(isVideoCall = true)

                            sendMessage(rtc { type = Rtc.Type.READY })
                        } else if (viewState is ViewState.AudioDialog) {
                            viewState = ViewState.AudioDialog(State.PREPARATION)

                            initializeCallConnection(isVideoCall = false)

                            sendMessage(rtc { type = Rtc.Type.READY })
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

                        closeLiveCall()

                        setNewStateByPreviousState(State.IDLE)
                    }
                    else -> {
                        return@on
                    }
                }
                return@on
            }

            if (!text.isNullOrBlank()) {
                logDebug("fetched text: $text")

                if (dialog.isOnLive) {
                    runOnUiThread {
                        chatAdapter?.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                        scrollToBottom()
                    }
                } else {
                    if (viewState is ViewState.VideoDialog) {
                        val queued = message.optInt("queued")

                        runOnUiThread {
                            videoCallView?.setInfoText(text)
                        }

                        if (queued > 1) {
                            runOnUiThread {
                                videoCallView?.setPendingQueueCount(queued)
                            }
                        }
                    } else if (viewState is ViewState.AudioDialog) {
                        val queued = message.optInt("queued")

                        runOnUiThread {
                            audioCallView?.setInfoText(text)
                        }

                        if (queued > 1) {
                            runOnUiThread {
                                audioCallView?.setPendingQueueCount(queued)
                            }
                        }
                    }

                    runOnUiThread {
                        chatAdapter?.addNewMessage(Message(Message.Type.OPPONENT, text, time))
                        chatFooterAdapter?.showGoToHomeButton()
                        scrollToBottom()
                    }

                    isLoading = false
                }
            }

            if (media != null) {
                val image = media.getNullableString("image")
                val file = media.getNullableString("file")
                val name = media.getNullableString("name")
                val ext = media.getNullableString("ext")

                if (!image.isNullOrBlank() && !ext.isNullOrBlank()) {
                    runOnUiThread {
                        chatAdapter?.addNewMessage(Message(
                            Message.Type.OPPONENT,
                            Media(imageUrl = image, name = name, ext = ext),
                            time
                        ))
                        scrollToBottom()
                    }
                }

                if (!file.isNullOrBlank() && !ext.isNullOrBlank()) {
                    runOnUiThread {
                        chatAdapter?.addNewMessage(Message(
                            Message.Type.OPPONENT,
                            Media(fileUrl = file, name = name, ext = ext),
                            time
                        ))
                        scrollToBottom()
                    }
                }
            }

            isLoading = false

        }?.on("category_list") { args ->

            if (args.size != 1) {
                return@on
            }

            val categoryList = args[0] as? JSONObject? ?: return@on

            val categoryListJson = categoryList.optJSONArray("category_list") ?: return@on

            logDebug("categoryList: $categoryList")

            if (chatBot.isLocked) return@on

            var currentCategories = mutableListOf<Category>()
            for (i in 0 until categoryListJson.length()) {
                (categoryListJson[i] as? JSONObject?)?.let { categoryJson ->
                    val parsed = parse(categoryJson)
                    currentCategories.add(parsed)
                }
            }

            currentCategories = currentCategories.sortedBy { it.id }.toMutableList()
            chatBot.allCategories.addAll(currentCategories)

            if (!chatBot.isBasicCategoriesFilled) {
                chatBot.allCategories.forEach { category ->
//                    logDebug("category: $category, ${category.parentId == null}")

                    if (category.parentId == null) {
                        sendUserDashboard(jsonObject {
                            put("action", "get_category_list")
                            put("parent_id", category.id)
                        })
                    }
                }

                chatBot.isBasicCategoriesFilled = true
            }

//            logDebug("------------------------------------------------------------")
//
//            logDebug("categories: ${chatBot.allCategories}")
//
//            logDebug("------------------------------------------------------------")

            if (chatBot.activeCategory != null) {
                if (chatBot.activeCategory?.children?.containsAll(currentCategories) == false) {
                    chatBot.activeCategory?.children?.addAll(currentCategories)
                }
                val messages = listOf(Message(Message.Type.CROSS_CHILDREN, chatBot.activeCategory))
                runOnUiThread {
                    chatAdapter?.setNewMessages(messages)
                }
            }

            isLoading = false

        }?.on(Socket.EVENT_DISCONNECT) {
            logDebug("event [EVENT_DISCONNECT]")

            closeLiveCall()

            if (viewState is ViewState.VideoDialog || viewState is ViewState.AudioDialog) {
                setNewStateByPreviousState(State.IDLE)
            } else {
                viewState = ViewState.ChatBot
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

                sendMessage(rtc { type = Rtc.Type.ANSWER; sdp = sessionDescription.description })
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

                sendMessage(rtc { type = Rtc.Type.OFFER; sdp = sessionDescription.description })
            }

            override fun onCreateFailure(s: String) {
                super.onCreateFailure(s)
                logDebug("onCreateFailure: $s")
            }
        }, mediaConstraints)
    }

    private fun createPeerConnection(factory: PeerConnectionFactory): PeerConnection? {
        var iceServers = iceServers.map { IceServer(it.url, it.username, it.credential) }

        if (iceServers.isNullOrEmpty()) {
            iceServers = listOf(IceServer("stun:stun.l.google.com:19302"))
        }

        val rtcConfig = RTCConfiguration(iceServers)
        rtcConfig.iceTransportsType = IceTransportsType.RELAY

        val peerConnectionConstraints = MediaConstraints()

        val peerConnectionObserver = object : Observer {
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}

            override fun onSignalingChange(signalingState: SignalingState) {
                logDebug("onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                logDebug("onIceConnectionChange: $iceConnectionState")

                when (iceConnectionState) {
                    IceConnectionState.COMPLETED -> {
                        runOnUiThread {
                            footerView?.enableAttachmentButton()
                        }
                    }
                    IceConnectionState.CLOSED -> {
                        dialog.clear()

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

                sendMessage(rtc {
                    type = Rtc.Type.CANDIDATE
                    id = iceCandidate.sdpMid
                    label = iceCandidate.sdpMLineIndex
                    candidate = iceCandidate.sdp
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
                    setSpeakerphoneOn(true)
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
                if (dialog.isInitiator) {
                    sendOffer()
                } else {
                    sendAnswer()
                }
            }
        }
        return factory.createPeerConnection(rtcConfig, peerConnectionConstraints, peerConnectionObserver)
    }

    private fun setSpeakerphoneOn(on: Boolean) {
        if (audioManager?.isSpeakerphoneOn == on) {
            return
        }
        audioManager?.isSpeakerphoneOn = on
    }

    private fun hangupLiveCall() {
        if (dialog.media == "text") {
            isSwitchToCallAgentClicked = false

            footerView?.disableAttachmentButton()

            dialog.clear()

            sendMessage(UserMessage.Action.FINISH)

            chatAdapter?.addNewMessage(
                Message(Message.Type.NOTIFICATION, getString(R.string.kenes_user_disconnected))
            )
        } else {
            footerView?.disableAttachmentButton()

            dialog.clear()

            sendMessage(rtc { type = Rtc.Type.HANGUP })
            sendMessage(UserMessage.Action.FINISH)

            chatAdapter?.addNewMessage(
                Message(Message.Type.NOTIFICATION, getString(R.string.kenes_user_disconnected))
            )
        }
    }

    private fun sendUserDashboard(jsonObject: JSONObject): Emitter? {
        jsonObject.put("lang", currentLanguage)
        return socket?.emit("user_dashboard", jsonObject)
    }

    private fun sendMessage(action: UserMessage.Action): Emitter? {
        return sendMessage(rtc = null, action = action)
    }

    private fun sendMessage(rtc: Rtc? = null, action: UserMessage.Action? = null): Emitter? {
        logDebug("sendMessage: $rtc; $action")
        val userMessage = UserMessage(rtc, action).toJsonObject()
        userMessage.put("lang", currentLanguage)
        return socket?.emit("message", userMessage)
    }

    private fun sendUserMessage(message: String, isInputClearText: Boolean = true): Emitter? {
        val emitter = socket?.emit("user_message", jsonObject { 
            put("text", message) 
            put("lang", currentLanguage)
        })

        if (isInputClearText) {
            footerView?.clearInputViewText()
        }

        chatAdapter?.addNewMessage(Message(Message.Type.USER, message), false)
        scrollToBottom()

        return emitter
    }

    private fun scrollToTop() {
        if ((recyclerView?.layoutManager as? LinearLayoutManager?)?.findFirstCompletelyVisibleItemPosition() == 0) {
            return
        }
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

                chatFooterAdapter?.clear()

                if (isLoading) {
                    isLoading = false
                }

                when (viewState.state) {
                    State.IDLE, State.USER_DISCONNECT -> {
                        headerView?.hideHangupButton()
                        headerView?.setOpponentInfo(configs.opponent)

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

//                        chatAdapter?.clearMessages()
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
                    State.OPPONENT_DISCONNECT, State.FINISHED -> {
                        headerView?.hideHangupButton()
                        headerView?.setOpponentInfo(configs.opponent)

                        videoCallView?.setDefaultState()
                        videoCallView?.visibility = View.GONE

                        feedbackView?.visibility = View.GONE

                        footerView?.setDefaultState()
                        footerView?.visibility = View.GONE

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

                        videoDialogView?.showControlButtons()
                        videoDialogView?.visibility = View.VISIBLE
                    }
                }
            }
            is ViewState.AudioDialog -> {
                infoView?.visibility = View.GONE

                chatFooterAdapter?.clear()

                if (isLoading) {
                    isLoading = false
                }

                when (viewState.state) {
                    State.IDLE, State.USER_DISCONNECT -> {
                        headerView?.hideHangupButton()
                        headerView?.setOpponentInfo(configs.opponent)

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

//                        chatAdapter?.clearMessages()
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
                    State.OPPONENT_DISCONNECT, State.FINISHED -> {
                        headerView?.hideHangupButton()
                        headerView?.setOpponentInfo(configs.opponent)

                        audioCallView?.setDefaultState()
                        audioCallView?.visibility = View.GONE

                        feedbackView?.visibility = View.GONE

                        footerView?.setDefaultState()
                        footerView?.visibility = View.GONE

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
                chatFooterAdapter?.clear()

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
                chatFooterAdapter?.clear()

                headerView?.hideHangupButton()
                headerView?.setOpponentInfo(configs.opponent)

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
                bottomNavigationView?.setHomeNavButtonActive()

                if (!isLoading) {
                    recyclerView?.visibility = View.VISIBLE
                }

                footerView?.setDefaultState()
                footerView?.visibility = View.VISIBLE
            }
            ViewState.Info -> {
                if (isLoading) {
                    isLoading = false
                }

                chatFooterAdapter?.clear()

                headerView?.hideHangupButton()
                headerView?.setOpponentInfo(configs.opponent)

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
        dialog.clear()

        runOnUiThread {
            footerView?.disableAttachmentButton()
        }

        isSwitchToCallAgentClicked = false

        setSpeakerphoneOn(false)

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

//        localVideoSource?.dispose()
        localVideoSource = null

//        localVideoSource?.dispose()
        localAudioSource = null

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

//        localAudioTrack?.dispose()
        localAudioTrack = null

//        remoteAudioTrack?.dispose()
        remoteAudioTrack = null

        videoDialogView?.release()
    }

    override fun onDestroy() {
        super.onDestroy()

        footerView?.disableAttachmentButton()

        chatRecyclerState = null

        isLoading = false

        isUserPromptMode = false

        viewState = ViewState.ChatBot

        closeLiveCall()

        iceServers = emptyList()

        configs.clear()
        chatBot.clear()
        dialog.clear()

        audioManager = null

        socket?.disconnect()
        socket = null

        headerView = null

        audioCallView?.setDefaultState()
        audioCallView = null

        audioDialogView?.setDefaultState()
        audioDialogView = null

        videoCallView?.setDefaultState()
        videoCallView = null

        videoDialogView?.setDefaultState()
        videoDialogView = null

        footerView?.setDefaultState()
        footerView = null

        chatAdapter?.callback = null
        chatAdapter?.clear()
        chatFooterAdapter?.callback = null
        chatFooterAdapter?.clear()
        chatAdapter?.let { mergeAdapter?.removeAdapter(it) }
        chatFooterAdapter?.let { mergeAdapter?.removeAdapter(it) }
        mergeAdapter = null
        recyclerView?.adapter = null
        recyclerView = null

        progressView = null

        rootView = null
    }

    private fun logDebug(message: String) {
        if (message.length > 4000) {
            Log.d(TAG, message.substring(0, 4000))
            logDebug(message.substring(4000))
        } else {
            Log.d(TAG, message)
        }
    }

}