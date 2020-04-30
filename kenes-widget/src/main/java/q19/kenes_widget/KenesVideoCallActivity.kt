package q19.kenes_widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.PeerConnection.Observer
import q19.kenes_widget.adapter.ChatAdapter
import q19.kenes_widget.adapter.ChatAdapterItemDecoration
import q19.kenes_widget.adapter.RatingAdapter
import q19.kenes_widget.model.*
import q19.kenes_widget.model.Category
import q19.kenes_widget.model.Configs
import q19.kenes_widget.model.Message
import q19.kenes_widget.model.RatingButton
import q19.kenes_widget.model.WidgetIceServer
import q19.kenes_widget.util.CircleTransformation
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.hideKeyboard
import java.lang.Exception
import kotlin.collections.ArrayList

class KenesVideoCallActivity : AppCompatActivity() {

    companion object {
        const val TAG = "LOL"

        private const val REQUEST_CODE_PERMISSIONS = 111

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, KenesVideoCallActivity::class.java)
        }
    }

    private var opponentAvatarView: AppCompatImageView? = null
    private var opponentNameView: TextView? = null
    private var opponentSecondNameView: TextView? = null
    private var videoCallView: LinearLayout? = null
    private var videoCallButton: AppCompatButton? = null
    private var videoCallInfoView: TextView? = null
    private var footerView: LinearLayout? = null
    private var inputView: AppCompatEditText? = null
    private var attachmentButton: AppCompatImageButton? = null
    private var recyclerView: RecyclerView? = null
    private var feedbackView: LinearLayout? = null
    private var titleView: TextView? = null
    private var ratingView: RecyclerView? = null
    private var rateButton: AppCompatButton? = null
    private var homeNavButton: AppCompatImageButton? = null
    private var videoNavButton: AppCompatImageButton? = null
    private var audioNavButton: AppCompatImageButton? = null
    private var infoNavButton: AppCompatImageButton? = null

    private var activeNavButtonIndex = 0

    private val navButtons
        get() = listOf(homeNavButton, videoNavButton, audioNavButton, infoNavButton)

    private lateinit var chatAdapter: ChatAdapter

    private var socket: Socket? = null
    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null

    private var palette = intArrayOf()

    private var permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private var configs: Configs = Configs()
    private var iceServers: MutableList<WidgetIceServer> = mutableListOf()
    private var messages: MutableList<Message> = mutableListOf()
    private var activeDialog: Dialog? = null

    private var isCategoriesShown: Boolean = false
    private var isFilled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_video_call)

        // TODO: Remove later, exhaustive on PROD
        UrlUtil.HOSTNAME = "https://kenes.vlx.kz"

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

        // ------------------------------------------------------------------------


        // --------------------- Default screen setups ----------------------------

        /**
         * Default active navigation button [homeNavButton]
         */
        setActiveNavButtonTintColor(homeNavButton)

        /**
         * Default states of views
         */
        rateButton?.isEnabled = false
        bindOpponentData(Configs())
        inputView?.text?.clear()

        // TODO: Remove after attachment upload ability realization
        attachmentButton?.visibility = View.GONE

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

                isCategoriesShown = false
                isFilled = false

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
            val initialize = JSONObject()
            initialize.put("video", true)
            socket?.emit("initialize", initialize)

            videoCall()
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

        inputView?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = v.text.toString()
                sendUserTextMessage(text)
                inputView?.text?.clear()
                chatAdapter.addNewMessage(Message(Message.Type.SELF, text))
                scrollToBottom()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        chatAdapter = ChatAdapter()
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

        rootEglBase = EglBase.create()

        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true)
        peerConnectionFactory = PeerConnectionFactory(null)
        peerConnectionFactory?.setVideoHwAccelerationOptions(
            rootEglBase?.eglBaseContext,
            rootEglBase?.eglBaseContext
        )

        peerConnectionFactory?.let {
            peerConnection = createPeerConnection(it)
        }
    }

    private fun fetchWidgetConfigs() {
        try {
            val asyncTask = HttpRequestHandler(url = UrlUtil.HOSTNAME + "/configs")
            val response = asyncTask.execute().get()

            val json = JSONObject(response)

            val configs = json.optJSONObject("configs")
            val contacts = json.optJSONObject("contacts")
            val localBotConfigs = json.optJSONObject("local_bot_configs")

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
            e.printStackTrace()
        }
    }

    private fun fetchIceServers() {
        try {
            val asyncTask = HttpRequestHandler(url = UrlUtil.HOSTNAME + "/ice_servers")
            val response = asyncTask.execute().get()

            val json = JSONObject(response)

            val iceServersJson = json.optJSONArray("ice_servers")

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
            e.printStackTrace()
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
        socket = IO.socket(UrlUtil.SIGNALLING_SERVER_URL)

        socket?.on(Socket.EVENT_CONNECT) { args ->
            
            logD("event [EVENT_CONNECT]: $args")

            args.forEach { arg ->
                logD("event [EVENT_CONNECT], arg: ${arg as? JSONObject}")
            }

            val userDashboard = JSONObject()
            userDashboard.put("action", "get_category_list")
            userDashboard.put("parent_id", 0)
            socket?.emit("user_dashboard", userDashboard)

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
                        runOnUiThread {
                            activeDialog = Dialog(
                                operatorId = call.optString("operator"),
                                instance = call.optString("instance"),
                                media = call.optString("media")
                            )

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
                logD("categoryListJson: $categoryList")

                val categoryListJson = categoryList?.optJSONArray("category_list")

                if (categoryListJson != null) {
                    var categories = mutableListOf<Category>()
                    for (i in 0 until categoryListJson.length()) {
                        val categoryJson = categoryListJson[i] as JSONObject
                        categories.add(Category(
                            id = categoryJson.optLong("id"),
                            title = categoryJson.optString("title"),
                            lang = categoryJson.optInt("lang"),
                            parentId = categoryJson.optLong("parent_id", -1),
                            photo = categoryJson.optString("photo")
                        ))
                    }

                    categories = categories.sortedBy { it.id }.toMutableList()

                    categories.forEachIndexed { index, category ->
//                        logD("category: $category")

                        if (category.parentId == null || category.parentId == -1L) {
                            if (palette.isNotEmpty()) {
                                category.color = palette[index]
                            }
                            messages.add(Message(Message.Type.CATEGORY, category))

                            val userDashboard = JSONObject()
                            userDashboard.put("action", "get_category_list")
                            userDashboard.put("parent_id", category.id)
                            socket?.emit("user_dashboard", userDashboard)
                        } else {
                            messages.map {
                                if (it.category?.id == category.parentId) {
                                    it.category?.sections?.add(category)
                                }
                            }
                            isFilled = true
                        }
                    }

                    val isMessagesNotEmpty = !messages.isNullOrEmpty() && messages.all { !it.category?.sections.isNullOrEmpty() }

                    if (!isCategoriesShown && isFilled && isMessagesNotEmpty) {
//                        logD("FINAL ->>>>>>: " + messages.map { it.category?.id.toString() + " - " + it.category?.title + " -> " + it.category?.sections?.map { section -> section.id.toString() + " - " + section.title + " #" + section.parentId  }})
                        runOnUiThread {
                            feedbackView?.visibility = View.GONE
                            videoCallView?.visibility = View.GONE
                            recyclerView?.visibility = View.VISIBLE
                            footerView?.visibility = View.VISIBLE

                            chatAdapter.setNewMessages(this.messages)
                            scrollToTop()

                            isCategoriesShown = true
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

                if (message != null) {
                    val type = message.optString("type")
                    val text = message.optString("text")
                    val noOnline = message.optBoolean("no_online")
                    val id = message.optString("id")
                    val action = message.optString("action")
                    val time = message.optLong("time")
                    val sender = message.optString("sender")

                    if (noOnline) {
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("Внимание")
                                .setMessage(text)
                                .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
//                                    peerConnectionFactory?.dispose()
//                                    peerConnection?.dispose()
//                                    rootEglBase?.release()
//                                    socket?.close()

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
                        if (sender == activeDialog?.operatorId) {
                            if (!id.isNullOrBlank()) {
                                runOnUiThread {
                                    activeDialog = null

                                    videoCallButton?.isEnabled = false
                                    videoCallInfoView?.text = text
                                    videoCallInfoView?.visibility = View.VISIBLE

                                    chatAdapter.addNewMessage(
                                        Message(Message.Type.OPPONENT, text, time)
                                    )
                                    scrollToBottom()
                                }
                            } else {
                                if (type == "offer") {
                                    logD("connectToSignallingServer: OFFER")
                                } else if (type == "accept") {
                                    logD("connectToSignallingServer: ACCEPT:")
                                    val rtc = message.optJSONObject("rtc")
                                    logD("connectToSignallingServer: RTC: $rtc")
                                }
                            }
                        } else {
                            Log.w(TAG, "WTF? Sender and call agent ids are DIFFERENT! sender: $sender, id: ${activeDialog?.operatorId}")
                        }
                    }
                }
            }

        }?.on(Socket.EVENT_DISCONNECT) {

            logD("event [EVENT_DISCONNECT]")

            runOnUiThread {
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

    private fun videoCall() {
        val sdpMediaConstraints = MediaConstraints()

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                logD("onCreateSuccess: " + sessionDescription.description)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)

                val message = JSONObject()
                message.put("type", "offer")
                message.put("sdp", sessionDescription.description)
                socket?.emit("message", message)

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
            iceServers.add(IceServer("stun:stun.l.google.com:19302"))
        }
        val rtcConfig = RTCConfiguration(iceServers)
        val pcConstraints = MediaConstraints()
        val pcObserver: Observer = object : Observer {
            override fun onSignalingChange(signalingState: SignalingState) {
                logD("onSignalingChange: $signalingState")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                logD("onIceConnectionChange: $iceConnectionState")
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                logD("onIceConnectionReceivingChange: $b")
            }

            override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
                logD("onIceGatheringChange: $iceGatheringState")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                logD("onIceCandidate: " + iceCandidate.sdp)

                val message = JSONObject()
                message.put("type", "candidate")
                message.put("label", iceCandidate.sdpMLineIndex)
                message.put("id", iceCandidate.sdpMid)
                message.put("candidate", iceCandidate.sdp)
                logD("onIceCandidate: sending candidate $message")
                socket?.emit("message", message)
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                logD("onIceCandidatesRemoved: " + iceCandidates.contentToString())
            }

            override fun onAddStream(mediaStream: MediaStream) {
                logD("onAddStream: " + mediaStream.videoTracks.size)
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                logD("onRemoveStream: $mediaStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                logD("onDataChannel: $dataChannel")
            }

            override fun onRenegotiationNeeded() {
                logD("onRenegotiationNeeded")
            }
        }
        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver)
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

    override fun onDestroy() {
        super.onDestroy()

        activeDialog = null
        isFilled = false
        isCategoriesShown = false
        messages.clear()

        socket?.disconnect()
        peerConnectionFactory?.dispose()
        peerConnection?.dispose()
        rootEglBase?.release()

        socket = null
        peerConnectionFactory = null
        peerConnection = null
        rootEglBase = null

        opponentAvatarView = null
        opponentNameView = null
        opponentSecondNameView = null
        videoCallView = null
        videoCallButton = null
        videoCallInfoView = null
        inputView = null

        chatAdapter.clearMessages()
        recyclerView?.adapter = null
        recyclerView = null
    }

    private fun logD(message: String) {
        Log.d(TAG, message)
    }

}