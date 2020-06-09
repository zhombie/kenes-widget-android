package q19.kenes_widget.network.socket

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import q19.kenes_widget.model.*
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.jsonObject
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.UrlUtil

internal class SocketClient {

    companion object {
        const val TAG = "SocketClient"
    }

    private var socket: Socket? = null

    private var onConnect: Emitter.Listener? = null

    var listener: Listener? = null

    private val onCall = Emitter.Listener { args ->
//        debug(TAG, "event [CALL]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[JSONObject] data: $data")

        val type = data.optString("type")
        val media = data.optString("media")
        val operator = data.optString("operator")
        val instance = data.optString("instance")

        listener?.onCall(type, media, operator, instance)
    }

    private val onOperatorGreet = Emitter.Listener { args ->
//        debug(TAG, "event [OPERATOR_GREET]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[JSONObject] data: $data")

//        val name = data.optString("name")
        val fullName = data.optString("full_name")
        val photo = data.optString("photo")
        val text = data.optString("text")

        val photoUrl = UrlUtil.getStaticUrl(photo)

        listener?.onCallAgentGreet(fullName, photoUrl, text)
    }

    private val onFormInit = Emitter.Listener { args ->
//        debug(TAG, "event [FORM_INIT]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[JSONObject] data: $data")

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

    private val onFeedback = Emitter.Listener { args ->
//        debug(TAG, "event [FEEDBACK]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[JSONObject] data: $data")

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

    private val onUserQueue = Emitter.Listener { args ->
//        debug(TAG, "event [USER_QUEUE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[JSONObject] data: $data")

        val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

        listener?.onPendingUsersQueueCount(count = count)
    }

    private val onMessage = Emitter.Listener { args ->
//        debug(TAG, "event [MESSAGE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[JSONObject] data: $data")

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
                RTC.Type.START?.value -> listener?.onRTCStart()
                RTC.Type.PREPARE?.value -> listener?.onRTCPrepare()
                RTC.Type.READY?.value -> listener?.onRTCReady()
                RTC.Type.OFFER?.value ->
                    listener?.onRTCOffer(
                        SessionDescription(
                            parseRTCType(rtc.getString("type")),
                            rtc.getString("sdp")
                        )
                    )
                RTC.Type.ANSWER?.value ->
                    listener?.onRTCAnswer(
                        SessionDescription(
                            parseRTCType(rtc.getString("type")),
                            rtc.getString("sdp")
                        )
                    )
                RTC.Type.CANDIDATE?.value ->
                    listener?.onRTCIceCandidate(
                        IceCandidate(
                            rtc.getString("id"),
                            rtc.getInt("label"),
                            rtc.getString("candidate")
                        )
                    )
                RTC.Type.HANGUP?.value -> listener?.onRTCHangup()
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

    private val onCategoryList = Emitter.Listener { args ->
//        debug(TAG, "event [CATEGORY_LIST]")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        val categoryListJson = data.optJSONArray("category_list") ?: return@Listener

//        debug(TAG, "categoryList: $data")

        val currentCategories = mutableListOf<Category>()
        for (i in 0 until categoryListJson.length()) {
            (categoryListJson[i] as? JSONObject?)?.let { categoryJson ->
                val parsed = parse(categoryJson)
                currentCategories.add(parsed)
            }
        }

        listener?.onCategories(currentCategories)
    }

    private val onDisconnect = Emitter.Listener {
//        debug(TAG, "event [EVENT_DISCONNECT]")

        listener?.onDisconnect()
    }

    fun start(url: String, language: String) {
        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3

        socket = IO.socket(url, options)

        onConnect = Emitter.Listener {
            debug(TAG, "event [EVENT_CONNECT]")

            getBasicCategories(language)

            listener?.onConnect()
        }

        socket?.on(Socket.EVENT_CONNECT, onConnect)
        socket?.on("call", onCall)
        socket?.on("operator_greet", onOperatorGreet)
        socket?.on("form_init", onFormInit)
        socket?.on("feedback", onFeedback)
        socket?.on("user_queue", onUserQueue)
        socket?.on("message", onMessage)
        socket?.on("category_list", onCategoryList)
        socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)

        socket?.connect()
    }

    fun textCall(language: String) {
        socket?.emit("initialize", jsonObject {
            put("video", false)
            put("lang", language)
        })
    }

    fun audioCall(language: String) {
        socket?.emit("initialize", jsonObject {
            put("audio", true)
            put("lang", language)
        })
    }

    fun videoCall(language: String) {
        socket?.emit("initialize", jsonObject {
            put("video", true)
            put("lang", language)
        })
    }

    fun getBasicCategories(language: String) {
        getCategories(0, language)
    }

    fun getCategories(parentId: Long, language: String) {
//        debug(TAG, "requestCategories: $parentId")

        socket?.emit("user_dashboard", jsonObject {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", language)
        })
    }

    fun getResponse(id: Int, language: String) {
//        debug(TAG, "requestResponse: $id")

        socket?.emit("user_dashboard", jsonObject {
            put("action", "get_response")
            put("id", id)
            put("lang", language)
        })
    }

    fun sendFeedback(rating: Int, chatId: Long) {
        socket?.emit("user_feedback", jsonObject {
            put("r", rating)
            put("chat_id", chatId)
        })
    }

    fun sendUserMessage(message: String, language: String) {
//        debug(TAG, "sendUserMessage -> message: $message")

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

    fun sendMessage(rtc: RTC? = null, action: UserMessage.Action? = null, language: String): Emitter? {
        val userMessage = UserMessage(rtc, action).toJsonObject()
        userMessage.put("lang", language)

//        debug(TAG, "sendMessage -> userMessage: $userMessage")

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

    fun userDisconnect() {
        socket?.emit("user_disconnect")
    }

    fun release() {
        onConnect = null

        socket?.off("call", onCall)
        socket?.off("operator_greet", onOperatorGreet)
        socket?.off("form_init", onFormInit)
        socket?.off("feedback", onFeedback)
        socket?.off("user_queue", onUserQueue)
        socket?.off("message", onMessage)
        socket?.off("category_list", onCategoryList)
        socket?.disconnect()
        socket = null
    }

    private fun parseRTCType(type: String): SessionDescription.Type? {
        return when (type) {
            "offer" -> SessionDescription.Type.OFFER
            "answer" -> SessionDescription.Type.ANSWER
            else -> null
        }
    }

    interface Listener {
        fun onConnect()

        fun onCall(type: String, media: String, operator: String, instance: String)
        fun onCallAgentGreet(fullName: String, photoUrl: String?, text: String)
        fun onFormInit(dynamicForm: DynamicForm)
        fun onFeedback(text: String, ratingButtons: List<RatingButton>)
        fun onPendingUsersQueueCount(text: String? = null, count: Int)
        fun onNoOnlineCallAgents(text: String): Boolean
        fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
        fun onNoResultsFound(text: String, timestamp: Long): Boolean
        fun onCallAgentDisconnected(text: String, timestamp: Long): Boolean

        fun onRTCStart()
        fun onRTCPrepare()
        fun onRTCReady()
        fun onRTCAnswer(sessionDescription: SessionDescription)
        fun onRTCOffer(sessionDescription: SessionDescription)
        fun onRTCIceCandidate(iceCandidate: IceCandidate)
        fun onRTCHangup()

        fun onTextMessage(text: String, attachments: List<Attachment>? = null, timestamp: Long)
        fun onMediaMessage(media: Media, timestamp: Long)

        fun onCategories(categories: List<Category>)

        fun onDisconnect()
    }

}