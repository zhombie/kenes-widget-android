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
import androidx.appcompat.widget.AppCompatImageView
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
import q19.kenes_widget.models.FeedbackButton
import q19.kenes_widget.models.Message
import q19.kenes_widget.util.CircleTransformation
import q19.kenes_widget.util.UrlUtil
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
    private var inputView: AppCompatEditText? = null
    private var recyclerView: RecyclerView? = null

    private var chatAdapter: ChatAdapter? = null

    private var socket: Socket? = null
    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null

    private var permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

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
        inputView = findViewById(R.id.inputView)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView?.visibility = View.GONE
        callView?.visibility = View.VISIBLE

        callButton?.setOnClickListener {
            start()
        }

        inputView?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserTextMessage(v.text.toString())
                inputView?.text?.clear()
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

    private fun connectToSignallingServer() {
        socket = IO.socket(SIGNALLING_SERVER_URL)

        logD("connectToSignallingServer")

        socket?.on(Socket.EVENT_CONNECT) { args ->
            
            logD("connectToSignallingServer: EVENT_CONNECT: $args")
            logD("connectToSignallingServer -> socket.connected(): " + socket?.connected())

            val initialize = JSONObject()
            initialize.put("video", true)
            socket?.emit("initialize", initialize)
            
            videoCall()

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

        }?.on("operator_greet") { args ->

            logD("connectToSignallingServer: OPERATOR_GREET: $args")
            if (args.size == 1) {
                val operatorGreet = args[0] as? JSONObject?
                logD("connectToSignallingServer: JSONObject operatorGreet: $operatorGreet")

                if (operatorGreet != null) {
                    val name = operatorGreet.optString("name")
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

                        text = text.replace("{}", name)
                        chatAdapter?.addNewItem(Message(false, text))
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
                    val chatId = feedback.optLong("chat_id")

                    if (buttons != null) {
                        val feedbackButtons = mutableListOf<FeedbackButton>()
                        for (i in 0..buttons.length()) {
                            val button = buttons[i] as JSONObject
                            val feedbackButton = FeedbackButton(
                                button.optString("title"),
                                button.optString("payload")
                            )
                            feedbackButtons.add(feedbackButton)
                        }
                        feedbackButtons.forEach {
                            logD("feedbackButton: " + it.title + ", " + it.payload)
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
                                    dialog.dismiss()
                                    socket?.close()
                                    peerConnectionFactory?.dispose()
                                    peerConnection?.dispose()
                                    rootEglBase?.release()
                                }
                                .show()
                        }
                    } else if (!action.isNullOrBlank() && action == "operator_disconnect") {
                        runOnUiThread {
                            chatAdapter?.clearItems()
                            recyclerView?.visibility = View.GONE
                            callView?.visibility = View.VISIBLE
                            callButton?.isEnabled = true
                            infoView?.text = null
                            infoView?.visibility = View.GONE
                        }
                    } else {
                        if (!id.isNullOrBlank()) {
                            runOnUiThread {
                                callButton?.isEnabled = false
                                infoView?.text = text
                                infoView?.visibility = View.VISIBLE

                                chatAdapter?.addNewItem(Message(false, text, time))
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
        chatAdapter?.addNewItem(Message(true, text))
    }

    private fun logD(message: String) {
        Log.d(TAG, message)
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

        chatAdapter = null
        recyclerView?.adapter = null
        recyclerView = null
    }

}