package q19.kenes_widget.network.socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import q19.kenes_widget.model.*
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.jsonObject
import q19.kenes_widget.util.UrlUtil

internal class SocketClient(url: String, language: String) {

    companion object {
        const val TAG = "SocketClient"
    }

    private var socket: Socket? = null

    var listener: Listener? = null

    private val eventConnectEmitter = Emitter.Listener { args ->
        logDebug("event [EVENT_CONNECT]: $args")

        requestCategories(0, language)

        listener?.onSocketConnect()
    }

    private val eventCallEmitter = Emitter.Listener { args ->
        logDebug("event [CALL]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        logDebug("[JSONObject] data: $data")

        val type = data.optString("type")
        val media = data.optString("media")
        val operator = data.optString("operator")
        val instance = data.optString("instance")

        listener?.onCall(type, media, operator, instance)
    }

    private val eventOperatorGreetEmitter = Emitter.Listener { args ->
        logDebug("event [OPERATOR_GREET]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        logDebug("[JSONObject] data: $data")

//        val name = data.optString("name")
        val fullName = data.optString("full_name")
        val photo = data.optString("photo")
        val text = data.optString("text")

        val photoUrl = UrlUtil.getStaticUrl(photo)

        listener?.onCallAgentGreet(fullName, photoUrl, text)
    }

    private val eventFormInitEmitter = Emitter.Listener { args ->
        logDebug("event [FORM_INIT]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        logDebug("[JSONObject] data: $data")

        val formJson = data.getJSONObject("form")
        val formFieldsJsonArray = data.getJSONArray("form_fields")

        val dynamicFormFields = mutableListOf<DynamicFormField>()
        for (i in 0 until formFieldsJsonArray.length()) {
            val formFieldJson = formFieldsJsonArray[i] as JSONObject
            dynamicFormFields.add(DynamicFormField(
                id = formFieldJson.getLong("id"),
                title = formFieldJson.getNullableString("title"),
                prompt = formFieldJson.getNullableString("prompt"),
                type = formFieldJson.getString("type"),
                default = formFieldJson.getNullableString("default"),
                formId = formFieldJson.getLong("form_id"),
                level = formFieldJson.optInt("level", -1)
            ))
        }

        val form = DynamicForm(
            id = formJson.getLong("id"),
            title = formJson.getNullableString("title"),
            isFlex = formJson.optInt("is_flex"),
            prompt = formJson.getNullableString("prompt"),
            fields = dynamicFormFields
        )

        listener?.onFormInit(form)
    }

    private val eventFeedbackEmitter = Emitter.Listener { args ->
        logDebug("event [FEEDBACK]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        logDebug("[JSONObject] data: $data")

        val buttonsJson = data.optJSONArray("buttons")

        val text = data.optString("text")
//        val chatId = feedback.optLong("chat_id")

        if (buttonsJson != null) {
            val ratingButtons = mutableListOf<RatingButton>()
            for (i in 0 until buttonsJson.length()) {
                val button = buttonsJson[i] as JSONObject
                ratingButtons.add(
                    RatingButton(
                        button.optString("title"),
                        button.optString("payload")
                    )
                )
            }

            listener?.onFeedback(text, ratingButtons)
        }
    }

    private val eventUserQueueEmitter = Emitter.Listener { args ->
        logDebug("event [USER_QUEUE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        logDebug("[JSONObject] data: $data")

        val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

        listener?.onPendingUsersQueueCount(count = count)
    }

    private val eventMessageEmitter = Emitter.Listener { args ->
        logDebug("event [MESSAGE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        logDebug("[JSONObject] data: $data")

        val text = data.getNullableString("text")?.trim()
        val noOnline = data.optBoolean("no_online")
        val noResults = data.optBoolean("no_results")
//        val id = message.optString("id")
        val action = data.getNullableString("action")
        val time = data.optLong("time")
        val sender = data.getNullableString("sender")
        val from = data.getNullableString("from")
        val media = data.optJSONObject("media")
        val rtc = data.optJSONObject("rtc")
        val fuzzyTask = data.optBoolean("fuzzy_task")
//        val form = message.optJSONObject("form")
        val attachments = data.optJSONArray("attachments")

        if (noResults && from.isNullOrBlank() && sender.isNullOrBlank() && action.isNullOrBlank() && !text.isNullOrBlank()) {
            val isHandled = listener?.onNoResultsFound(text, time)
            if (isHandled == true) return@Listener
        }

        if (fuzzyTask && !text.isNullOrBlank()) {
            val isHandled = listener?.onFuzzyTaskOffered(text, time)
            if (isHandled == true) return@Listener
        }

        if (noOnline && !text.isNullOrBlank()) {
            val isHandled = listener?.onNoOnlineCallAgents(text)
            if (isHandled == true) return@Listener
        }

        if (action == "operator_disconnect" && !text.isNullOrBlank()) {
            val isHandled = listener?.onCallAgentDisconnected(text, time)
            if (isHandled == true) return@Listener
        }

        if (rtc != null) {
            when (rtc.getNullableString("type")) {
                Rtc.Type.START?.value -> listener?.onRtcStart()
                Rtc.Type.PREPARE?.value -> listener?.onRtcPrepare()
                Rtc.Type.READY?.value -> listener?.onRtcReady()
                Rtc.Type.ANSWER?.value ->
                    listener?.onRtcAnswer(
                        parseRtcType(rtc.getString("type")),
                        rtc.getString("sdp")
                    )
                Rtc.Type.CANDIDATE?.value ->
                    listener?.onRtcIceCandidate(
                        IceCandidate(
                            rtc.getString("id"),
                            rtc.getInt("label"),
                            rtc.getString("candidate")
                        )
                    )
                Rtc.Type.OFFER?.value ->
                    listener?.onRtcOffer(
                        parseRtcType(rtc.getString("type")),
                        rtc.getString("sdp")
                    )
                Rtc.Type.HANGUP?.value -> listener?.onRtcHangup()
            }
            return@Listener
        }

        if (!text.isNullOrBlank()) {
            if (!data.isNull("queued")) {
                val queued = data.optInt("queued")
                listener?.onPendingUsersQueueCount(text, queued)
                listener?.onTextMessage(text = text, timestamp = time)
            } else {
                if (attachments != null) {
                    val newAttachments = mutableListOf<Attachment>()
                    for (i in 0 until attachments.length()) {
                        val attachment = attachments[i] as? JSONObject?
                        newAttachments.add(
                            Attachment(
                                title = attachment?.getNullableString("title"),
                                ext = attachment?.getNullableString("ext"),
                                type = attachment?.getNullableString("type"),
                                url = attachment?.getNullableString("url")
                            )
                        )
                    }

                    listener?.onTextMessage(text = text, attachments = newAttachments, timestamp = time)
                } else {
                    listener?.onTextMessage(text = text, timestamp = time)
                }
            }

            return@Listener
        }

        if (media != null) {
            val image = media.getNullableString("image")
            val file = media.getNullableString("file")
            val name = media.getNullableString("name")
            val ext = media.getNullableString("ext")

            if (!image.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onMediaMessage(Media(imageUrl = image, hash = name, ext = ext), time)
            }

            if (!file.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onMediaMessage(Media(fileUrl = file, hash = name, ext = ext), time)
            }
        }
    }

    private val eventCategoryListEmitter = Emitter.Listener { args ->
        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        val categoryListJson = data.optJSONArray("category_list") ?: return@Listener

        logDebug("categoryList: $data")

        val currentCategories = mutableListOf<Category>()
        for (i in 0 until categoryListJson.length()) {
            (categoryListJson[i] as? JSONObject?)?.let { categoryJson ->
                val parsed = parse(categoryJson)
                currentCategories.add(parsed)
            }
        }

        listener?.onCategories(currentCategories)
    }

    private val eventDisconnectEmitter = Emitter.Listener {
        logDebug("event [EVENT_DISCONNECT]")

        listener?.onSocketDisconnect()
    }

    init {
        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3

        socket = IO.socket(url, options)

        socket?.on(Socket.EVENT_CONNECT, eventConnectEmitter)
        socket?.on("call", eventCallEmitter)
        socket?.on("operator_greet", eventOperatorGreetEmitter)
        socket?.on("form_init", eventFormInitEmitter)
        socket?.on("feedback", eventFeedbackEmitter)
        socket?.on("user_queue", eventUserQueueEmitter)
        socket?.on("message", eventMessageEmitter)
        socket?.on("category_list", eventCategoryListEmitter)
        socket?.on(Socket.EVENT_DISCONNECT, eventDisconnectEmitter)

        socket?.connect()
    }

    fun textCallToCallAgent(language: String) {
        socket?.emit("initialize", jsonObject {
            put("video", false)
            put("lang", language)
        })
    }

    fun audioCallToCallAgent(language: String) {
        socket?.emit("initialize", jsonObject {
            put("audio", true)
            put("lang", language)
        })
    }

    fun videoCallToCallAgent(language: String) {
        socket?.emit("initialize", jsonObject {
            put("video", true)
            put("lang", language)
        })
    }

    fun requestCategories(parentId: Long, language: String) {
//        logDebug("requestCategories: $parentId")

        socket?.emit("user_dashboard", jsonObject {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", language)
        })
    }

    fun requestResponse(id: Int, language: String) {
//        logDebug("requestResponse: $id")

        socket?.emit("user_dashboard", jsonObject {
            put("action", "get_response")
            put("id", id)
            put("lang", language)
        })
    }

    fun sendFeedback(ratingButton: RatingButton) {
        socket?.emit("user_feedback", jsonObject {
            put("r", ratingButton.rating)
            put("chat_id", ratingButton.chatId)
        })
    }

    fun sendUserMessage(message: String, language: String) {
        socket?.emit("user_message", jsonObject {
            put("text", message)
            put("lang", language)
        })
    }

    fun sendUserMediaMessage(mediaType: String, url: String) {
        socket?.emit("user_message", jsonObject {
            put(mediaType, url)
        })
    }

    fun sendMessage(rtc: Rtc? = null, action: UserMessage.Action? = null, language: String): Emitter? {
        val userMessage = UserMessage(rtc, action).toJsonObject()
        userMessage.put("lang", language)
        return socket?.emit("message", userMessage)
    }

    fun sendFuzzyTaskConfirmation(name: String, email: String, phone: String) {
        socket?.emit("confirm_fuzzy_task", jsonObject {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("res", '1')
        })
    }

    fun sendUserLanguage(language: String) {
        socket?.emit("user_language", jsonObject {
            put("language", language)
        })
    }

    fun forceDisconnect() {
        socket?.emit("user_disconnect")
    }

    fun release() {
        socket?.off("call", eventCallEmitter)
        socket?.off("operator_greet", eventOperatorGreetEmitter)
        socket?.off("form_init", eventFormInitEmitter)
        socket?.off("feedback", eventFeedbackEmitter)
        socket?.off("user_queue", eventUserQueueEmitter)
        socket?.off("message", eventMessageEmitter)
        socket?.off("category_list", eventCategoryListEmitter)
        socket?.disconnect()
        socket = null
    }

    private fun parseRtcType(type: String): SessionDescription.Type? {
        return when (type) {
            "offer" -> SessionDescription.Type.OFFER
            "answer" -> SessionDescription.Type.ANSWER
            else -> null
        }
    }

    private fun logDebug(message: String) {
        if (message.length > 4000) {
            Log.d(TAG, message.substring(0, 4000))
            logDebug(message.substring(4000))
        } else {
            Log.d(TAG, message)
        }
    }

    interface Listener {
        fun onSocketConnect()

        fun onCall(type: String, media: String, operator: String, instance: String)
        fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String)
        fun onFormInit(dynamicForm: DynamicForm)
        fun onFeedback(text: String, ratingButtons: List<RatingButton>)
        fun onPendingUsersQueueCount(text: String? = null, count: Int)
        fun onNoOnlineCallAgents(text: String): Boolean
        fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
        fun onNoResultsFound(text: String, timestamp: Long): Boolean
        fun onCallAgentDisconnected(text: String, timestamp: Long): Boolean

        fun onRtcStart()
        fun onRtcPrepare()
        fun onRtcReady()
        fun onRtcOffer(type: SessionDescription.Type?, sessionDescription: String)
        fun onRtcAnswer(type: SessionDescription.Type?, sessionDescription: String)
        fun onRtcIceCandidate(iceCandidate: IceCandidate)
        fun onRtcHangup()

        fun onTextMessage(text: String, attachments: List<Attachment>? = null, timestamp: Long)
        fun onMediaMessage(media: Media, timestamp: Long)

        fun onCategories(categories: List<Category>)

        fun onSocketDisconnect()
    }

}