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
import q19.kenes_widget.model.Category
import q19.kenes_widget.model.Configs
import q19.kenes_widget.model.Message
import q19.kenes_widget.model.RatingButton
import q19.kenes_widget.util.CircleTransformation
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.hideKeyboard
import java.util.*

class KenesVideoCallActivity : AppCompatActivity() {

    companion object {
        const val TAG = "LOL"

        private const val REQUEST_CODE_PERMISSIONS = 111

        private const val SIGNALLING_SERVER_URL = "https://kenes2.vlx.kz/user"

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, KenesVideoCallActivity::class.java)
        }
    }

    private var avatarView: AppCompatImageView? = null
    private var nameView: TextView? = null
    private var subNameView: TextView? = null
    private var callView: LinearLayout? = null
    private var callButton: AppCompatButton? = null
    private var infoView: TextView? = null
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

    private var permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private var configs: Configs = Configs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kenes_activity_video_call)

        // TODO: Remove later, exhaustive on PROD
        UrlUtil.HOSTNAME = "https://kenes.vlx.kz"

        if (Picasso.get() == null) {
            Picasso.setSingletonInstance(Picasso.Builder(this).build())
        }

        avatarView = findViewById(R.id.avatarView)
        nameView = findViewById(R.id.nameView)
        subNameView = findViewById(R.id.subNameView)
        callView = findViewById(R.id.callView)
        callButton = findViewById(R.id.callButton)
        infoView = findViewById(R.id.infoView)
        footerView = findViewById(R.id.footerView)
        inputView = findViewById(R.id.inputView)
        attachmentButton = findViewById(R.id.attachmentButton)
        recyclerView = findViewById(R.id.recyclerView)
        feedbackView = findViewById(R.id.feedbackView)
        titleView = findViewById(R.id.titleView)
        ratingView = findViewById(R.id.ratingView)
        rateButton = findViewById(R.id.rateButton)
        homeNavButton = findViewById(R.id.homeButton)
        videoNavButton = findViewById(R.id.videoButton)
        audioNavButton = findViewById(R.id.audioButton)
        infoNavButton = findViewById(R.id.infoButton)

        setActiveNavButtonTintColor(homeNavButton)

        rateButton?.isEnabled = false

        recyclerView?.visibility = View.GONE
        feedbackView?.visibility = View.GONE
        callView?.visibility = View.VISIBLE
        footerView?.visibility = View.VISIBLE

        homeNavButton?.setOnClickListener {
            activeNavButtonIndex = 0
            updateActiveNavButtonTintColor()

            if (feedbackView?.visibility == View.VISIBLE) {
                return@setOnClickListener
            } else {
                chatAdapter.clearMessages()
                scrollToTop()

                recyclerView?.visibility = View.GONE
                feedbackView?.visibility = View.GONE
                footerView?.visibility = View.VISIBLE
                callView?.visibility = View.VISIBLE

                val userDashboard = JSONObject()
                userDashboard.put("action", "get_category_list")
                userDashboard.put("parent_id", 0)
                socket?.emit("user_dashboard", userDashboard)
            }
        }

        videoNavButton?.setOnClickListener {
            activeNavButtonIndex = 1
            updateActiveNavButtonTintColor()
        }

        audioNavButton?.setOnClickListener {
            activeNavButtonIndex = 2
            updateActiveNavButtonTintColor()
        }

        infoNavButton?.setOnClickListener {
            activeNavButtonIndex = 3
            updateActiveNavButtonTintColor()
        }

        callButton?.setOnClickListener {
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

//        inputView?.

        inputView?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = v.text.toString()
                sendUserTextMessage(text)
                inputView?.text?.clear()
                chatAdapter.addNewMessage(Message(Message.Type.SELF, text))
                scrollToBottom()
                return@setOnEditorActionListener true
            }
            false
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
        fetchConfigs()

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

    private fun fetchConfigs() {
        val asyncTask = HttpRequestHandler()
        val response = asyncTask.execute().get()
        val json = JSONObject(response)
        val configs = json.optJSONObject("configs")
        val contacts = json.optJSONObject("contacts")
        val localBotConfigs = json.optJSONObject("local_bot_configs")

        Picasso.get()
            .load(UrlUtil.getStaticUrl(configs?.optString("image")))
            .fit()
            .centerCrop()
            .transform(CircleTransformation())
            .into(avatarView)

        nameView?.text = configs?.optString("default_operator")
        subNameView?.text = configs?.optString("title")

        contacts?.keys()?.forEach {
            this.configs.contacts.add(Configs.Contact(
                it,
                (contacts[it] as? String?) ?: ""
            ))
        }

        this.configs.workingHours = Configs.WorkingHours.from(
            configs?.optString("message_kk"),
            configs?.optString("message_ru")
        )
    }

    private fun connectToSignallingServer() {
        socket = IO.socket(SIGNALLING_SERVER_URL)

        logD("connectToSignallingServer")

        socket?.on(Socket.EVENT_CONNECT) { args ->
            
            logD("connectToSignallingServer: EVENT_CONNECT: $args")
            logD("connectToSignallingServer -> socket.connected(): " + socket?.connected())

            val userDashboard = JSONObject()
            userDashboard.put("action", "get_category_list")
            userDashboard.put("parent_id", 0)
            socket?.emit("user_dashboard", userDashboard)

        }?.on("open") { args ->

            logD("connectToSignallingServer: OPEN: $args")

        }?.on("call") { args ->

            logD("connectToSignallingServer: CALL: $args")

            if (args.size == 1) {
                val call = args[0] as? JSONObject?
                logD("connectToSignallingServer: JSONObject call: $call")

                if (call != null) {
                    val type = call.optString("type")

                    if (type == "accept") {
                        runOnUiThread {
                            recyclerView?.visibility = View.VISIBLE
                            callView?.visibility = View.GONE
                        }
                    }
                }
            }

        }?.on("stream") { args ->

            logD("connectToSignallingServer: STREAM: $args")

        }?.on("user_chat_init") { args ->

            logD("connectToSignallingServer: USER_CHAT_INIT: $args")

            if (args.size == 1) {
                val chat = args[0] as? JSONObject?
                logD("connectToSignallingServer: chat: $chat")
            }

        }?.on("category_list") { args ->

            logD("connectToSignallingServer: CATEGORY_LIST: $args")

            if (args.size == 1) {
                val categoryList = args[0] as? JSONObject?
                logD("connectToSignallingServer: categoryList: $categoryList")

                val categoriesList = categoryList?.optJSONArray("category_list")

                if (categoriesList != null) {
                    var categories = mutableListOf<Category>()
                    for (i in 0 until categoriesList.length()) {
                        val category = categoriesList[i] as JSONObject
                        categories.add(Category(
                            category.optLong("id"),
                            category.optString("title"),
                            category.optInt("lang"),
                            category.optLong("parent_id"),
                            category.optString("photo")
                        ))
                    }

                    categories = categories.sortedBy { it.id }.toMutableList()

                    categories.forEach {
//                        socket?.emit("")
                        logD("category: ${it.id}, ${it.title}")
                    }
                }
            }

        }?.on("form_init") { args ->

            logD("connectToSignallingServer: FORM_INIT: $args")

            if (args.size == 1) {
                val form = args[0] as? JSONObject?
                logD("connectToSignallingServer: form: $form")
            }

        }?.on("form_final") { args ->

            logD("connectToSignallingServer: FORM_FINAL: $args")

            if (args.size == 1) {
                val form = args[0] as? JSONObject?
                logD("connectToSignallingServer: form: $form")
            }

        }?.on("send_configs") { args ->

            logD("connectToSignallingServer: SEND_CONFIGS: $args")

            if (args.size == 1) {
                val configs = args[0] as? JSONObject?
                logD("connectToSignallingServer: configs: $configs")
            }

        }?.on("operator_greet") { args ->

            logD("connectToSignallingServer: OPERATOR_GREET: $args")
            if (args.size == 1) {
                val operatorGreet = args[0] as? JSONObject?
                logD("connectToSignallingServer: JSONObject operatorGreet: $operatorGreet")

                if (operatorGreet != null) {
//                    val name = operatorGreet.optString("name")
                    val fullName = operatorGreet.optString("full_name")
                    val photo = operatorGreet.optString("photo")
                    var text = operatorGreet.optString("text")

                    val photoUrl = UrlUtil.getStaticUrl(photo)

                    logD("photoUrl: $photoUrl")

                    runOnUiThread {
                        Picasso.get()
                            .load(photoUrl)
                            .fit()
                            .centerCrop()
                            .transform(CircleTransformation())
                            .into(avatarView)

                        nameView?.text = fullName
                        subNameView?.text = getString(R.string.kenes_call_agent)

                        text = text.replace("{}", fullName)
                        chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text))
                        scrollToBottom()
                    }
                }
            }

        }?.on("operator_typing") { args ->

            logD("connectToSignallingServer: OPERATOR_TYPING: $args")
            if (args.size == 1) {
                val operatorTyping = args[0] as? JSONObject?
                logD("connectToSignallingServer: JSONObject operatorTyping: $operatorTyping")

                if (operatorTyping != null) {
                }
            }

        }?.on("feedback") { args ->

            logD("connectToSignallingServer: FEEDBACK: $args")
            if (args.size == 1) {
                val feedback = args[0] as? JSONObject?
                logD("connectToSignallingServer: JSONObject feedback: $feedback")

                if (feedback != null) {
                    val buttons = feedback.optJSONArray("buttons")
                    val text = feedback.optString("text")
//                    val chatId = feedback.optLong("chat_id")

                    if (buttons != null) {
                        val ratingButtons = mutableListOf<RatingButton>()
                        for (i in 0 until buttons.length()) {
                            val button = buttons[i] as JSONObject
                            ratingButtons.add(RatingButton(
                                button.optString("title"),
                                button.optString("payload")
                            ))
                        }

                        runOnUiThread {
                            callView?.visibility = View.GONE
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

                                callView?.visibility = View.GONE
                                feedbackView?.visibility = View.GONE
                                recyclerView?.visibility = View.VISIBLE
                                footerView?.visibility = View.VISIBLE

                                avatarView?.setImageDrawable(null)
                                nameView?.text = null
                                subNameView?.text = null
                            }
                        }
                    }
                }
            }

        }?.on("message") { args ->
            logD("connectToSignallingServer: MESSAGE: $args")

            if (args.size == 1) {
                val message = args[0] as? JSONObject?
                if (message != null) {
                    logD("connectToSignallingServer: JSONObject message: $message")

                    val type = message.optString("type")
                    val text = message.optString("text")
                    val noOnline = message.optBoolean("no_online")
                    val id = message.optString("id")
                    val action = message.optString("action")
                    val time = message.optLong("time")

                    logD("connectToSignallingServer: no_online: $noOnline")

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

                                    avatarView?.setImageDrawable(null)
                                    nameView?.text = null
                                    subNameView?.text = null

                                    recyclerView?.visibility = View.GONE
                                    callView?.visibility = View.VISIBLE
                                    callButton?.isEnabled = true
                                    infoView?.text = null
                                    infoView?.visibility = View.GONE

                                    dialog.dismiss()
                                }
                                .show()
                        }
                    } else if (!action.isNullOrBlank() && action == "operator_disconnect") {
                        runOnUiThread {
                            feedbackView?.visibility = View.GONE
                            callView?.visibility = View.GONE
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
                        if (!id.isNullOrBlank()) {
                            runOnUiThread {
                                callButton?.isEnabled = false
                                infoView?.text = text
                                infoView?.visibility = View.VISIBLE

                                chatAdapter.addNewMessage(Message(Message.Type.OPPONENT, text, time))
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
                    }
                }
            }

        }?.on(Socket.EVENT_DISCONNECT) {

            logD("connectToSignallingServer: EVENT_DISCONNECT")

            runOnUiThread {
                avatarView?.setImageDrawable(null)
                nameView?.text = null
                subNameView?.text = null

                recyclerView?.visibility = View.GONE
                callView?.visibility = View.VISIBLE
                callButton?.isEnabled = true
                infoView?.text = null
                infoView?.visibility = View.GONE
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
            }

            override fun onCreateFailure(s: String) {
                super.onCreateFailure(s)
                logD("onCreateFailure: $s")
            }
        }, sdpMediaConstraints)
    }

    private fun createPeerConnection(factory: PeerConnectionFactory): PeerConnection? {
        val iceServers = ArrayList<IceServer>()
        iceServers.add(IceServer("stun:stun.l.google.com:19302"))
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
                logD("scrollTo: " + (adapter.itemCount - 1))
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
        socket?.disconnect()
        peerConnectionFactory?.dispose()
        peerConnection?.dispose()
        rootEglBase?.release()

        socket = null
        peerConnectionFactory = null
        peerConnection = null
        rootEglBase = null

        avatarView = null
        nameView = null
        subNameView = null
        callView = null
        callButton = null
        infoView = null
        inputView = null

        recyclerView?.adapter = null
        recyclerView = null
    }

    private fun logD(message: String) {
        Log.d(TAG, message)
    }

}