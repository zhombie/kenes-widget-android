package q19.kenes_widget.data.network.socket

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import q19.kenes_widget.data.model.*
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.jsonObject
import q19.kenes_widget.util.JsonUtil.putIfValueNotNull
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.findEnumBy

internal class SocketClient constructor(
    private var socket: Socket? = null,
    private var language: String? = null,
    var listener: Listener? = null
) {

    companion object {
        const val TAG = "SocketClient"
    }

    fun setLanguage(language: String) {
        this.language = language
    }

    private val onConnect = Emitter.Listener {
        debug(TAG, "event [EVENT_CONNECT]")

        listener?.onConnect()
    }

    private val onOperatorGreet = Emitter.Listener { args ->
//        debug(TAG, "event [OPERATOR_GREET]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        debug(TAG, "[OPERATOR_GREET] data: $data")

//        val name = data.optString("name")
        val fullName = data.optString("full_name")
        val photo = data.optString("photo")
        val text = data.optString("text")

        val photoUrl = UrlUtil.getStaticUrl(photo)

        listener?.onOperatorGreet(fullName, photoUrl, text)
    }

    private val onFormInit = Emitter.Listener { args ->
//        debug(TAG, "event [FORM_INIT]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

//        debug(TAG, "[FORM_INIT] data: $data")

        val formJson = data.getJSONObject("form")
        val formFieldsJsonArray = data.getJSONArray("form_fields")

        val dynamicFormFields = mutableListOf<DynamicFormField>()
        for (i in 0 until formFieldsJsonArray.length()) {
            val formFieldJson = formFieldsJsonArray[i] as JSONObject
            dynamicFormFields.add(
                DynamicFormField(
                    id = formFieldJson.getLong("id"),
                    title = formFieldJson.getNullableString("title"),
                    prompt = formFieldJson.getNullableString("prompt"),
                    type = findEnumBy { it.value == formFieldJson.getString("type") } ?: DynamicFormField.Type.TEXT,
                    default = formFieldJson.getNullableString("default"),
                    formId = formFieldJson.getLong("form_id"),
                    configs = null,
                    level = formFieldJson.optInt("level", -1),
                    value = null
                )
            )
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

    private val onFormFinal = Emitter.Listener { args ->
//        debug(TAG, "event [FORM_FINAL]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        debug(TAG, "[FORM_FINAL] data: $data")

        val taskJson = data.optJSONObject("task")
        val trackId = taskJson?.getNullableString("track_id")
//        val message = data.getNullableString("message")
//        val success = data.optBoolean("success", false)

        listener?.onFormFinal(text = trackId ?: "")
    }

    private val onOperatorTyping = Emitter.Listener {
        debug(TAG, "event [OPERATOR_TYPING]")
    }

    private val onFeedback = Emitter.Listener { args ->
//        debug(TAG, "event [FEEDBACK]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        debug(TAG, "[FEEDBACK] data: $data")

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

//        debug(TAG, "[USER_QUEUE] data: $data")

        val count = data.getInt("count")
//            val channel = userQueue.getInt("channel")

        listener?.onPendingUsersQueueCount(count = count)
    }

    private val onMessage = Emitter.Listener { args ->
//        debug(TAG, "event [MESSAGE]: $args")

        if (args.size != 1) return@Listener

        val data = args[0] as? JSONObject? ?: return@Listener

        debug(TAG, "[MESSAGE] data: $data")

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
        val attachmentsJson = data.optJSONArray("attachments")
        val replyMarkupJson = data.optJSONObject("reply_markup")
        val formJson = data.optJSONObject("form")

        debug(TAG, "replyMarkupJson: $replyMarkupJson")

        var replyMarkup: Message.ReplyMarkup? = null
        if (replyMarkupJson != null) {
            val rows = mutableListOf<List<Message.ReplyMarkup.Button>>()

            val inlineKeyboard = replyMarkupJson.optJSONArray("inline_keyboard")
            debug(TAG, "inlineKeyboard: $inlineKeyboard")
            if (inlineKeyboard != null) {
                for (i in 0 until inlineKeyboard.length()) {
                    val row = inlineKeyboard[i] as? JSONArray?

                    debug(TAG, "row: $row")

                    val buttons = mutableListOf<Message.ReplyMarkup.Button>()
                    for (j in 0 until (row?.length() ?: 0)) {
                        val button = row?.get(j) as? JSONObject?
                        debug(TAG, "button: $button")

                        buttons.add(
                            Message.ReplyMarkup.Button(
                                text = button?.getString("text") ?: "",
                                callbackData = button?.getNullableString("callback_data"),
                                url = button?.getNullableString("url")
                            )
                        )
                    }
                    rows.add(buttons)
                }
            }

            replyMarkup = Message.ReplyMarkup(rows)
        }

        var dynamicForm: DynamicForm? = null
        if (formJson != null && formJson.has("id")) {
            dynamicForm = DynamicForm(
                id = formJson.optLong("id"),
                title = formJson.getNullableString("title"),
                prompt = formJson.getNullableString("prompt")
            )
        }

        if (noResults && from.isNullOrBlank() && sender.isNullOrBlank() && action.isNullOrBlank() && !text.isNullOrBlank()) {
            val isHandled = listener?.onNoResultsFound(text, time)
            if (isHandled == true) return@Listener
        }

        if (fuzzyTask && !text.isNullOrBlank()) {
            val isHandled = listener?.onFuzzyTaskOffered(text, time)
            if (isHandled == true) return@Listener
        }

        if (noOnline && !text.isNullOrBlank()) {
            val isHandled = listener?.onNoOnlineOperators(text)
            if (isHandled == true) return@Listener
        }

        if (action == "chat_timeout" && !text.isNullOrBlank()) {
            val isHandled = listener?.onChatTimeout(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == "operator_disconnect" && !text.isNullOrBlank()) {
            val isHandled = listener?.onOperatorDisconnected(text, time)
            if (isHandled == true) return@Listener
        }

        if (action == "redirect" && !text.isNullOrBlank()) {
            val isHandled = listener?.onUserRedirected(text, time)
            if (isHandled == true) return@Listener
        }

        if (rtc != null) {
            when (rtc.getNullableString("type")) {
                RTC.Type.START?.value -> {
                    when (action) {
                        "call_accept" -> listener?.onCallAccept()
                        "call_redirect" -> listener?.onCallAccept()
                        "call_redial" -> {
                        }
                    }
                }
                RTC.Type.PREPARE?.value -> listener?.onRTCPrepare()
                RTC.Type.READY?.value -> listener?.onRTCReady()
                RTC.Type.OFFER?.value ->
                    listener?.onRTCOffer(
                        SessionDescription(
                            RTC.Type.to(rtc.getString("type")),
                            rtc.getString("sdp")
                        )
                    )
                RTC.Type.ANSWER?.value ->
                    listener?.onRTCAnswer(
                        SessionDescription(
                            RTC.Type.to(rtc.getString("type")),
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

        if (!data.isNull("queued")) {
            val queued = data.optInt("queued")
            listener?.onPendingUsersQueueCount(text, queued)
            listener?.onTextMessage(
                text = text,
                replyMarkup = replyMarkup,
                dynamicForm = dynamicForm,
                timestamp = time
            )
        } else {
            if (attachmentsJson != null) {
                val attachments = mutableListOf<Attachment>()
                for (i in 0 until attachmentsJson.length()) {
                    val attachment = attachmentsJson[i] as? JSONObject?
                    attachments.add(
                        Attachment(
                            title = attachment?.getNullableString("title"),
                            ext = attachment?.getNullableString("ext"),
                            type = attachment?.getNullableString("type"),
                            url = attachment?.getNullableString("url")
                        )
                    )
                }

                listener?.onTextMessage(
                    text = text,
                    replyMarkup = replyMarkup,
                    attachments = attachments,
                    dynamicForm = dynamicForm,
                    timestamp = time
                )
            } else {
                listener?.onTextMessage(
                    text = text,
                    replyMarkup = replyMarkup,
                    dynamicForm = dynamicForm,
                    timestamp = time
                )
            }
        }

        if (media != null) {
            val image = media.getNullableString("image")
            val audio = media.getNullableString("audio")
            val file = media.getNullableString("file")
            val name = media.getNullableString("name")
            val ext = media.getNullableString("ext")

            if (!image.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onMediaMessage(
                    media = Media(imageUrl = image, hash = name, ext = ext),
                    replyMarkup = replyMarkup,
                    timestamp = time
                )
            }

            if (!audio.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onMediaMessage(
                    media = Media(audioUrl = audio, hash = name, ext = ext),
                    replyMarkup = replyMarkup,
                    timestamp = time
                )
            }

            if (!file.isNullOrBlank() && !ext.isNullOrBlank()) {
                listener?.onMediaMessage(
                    media = Media(fileUrl = file, hash = name, ext = ext),
                    replyMarkup = replyMarkup,
                    timestamp = time
                )
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
//                debug(TAG, "categoryJson: $categoryJson")
                val parsed = parse(categoryJson)
                currentCategories.add(parsed)
            }
        }

        listener?.onCategories(currentCategories.sortedBy { it.config?.order })
    }

    private val onDisconnect = Emitter.Listener {
//        debug(TAG, "event [EVENT_DISCONNECT]")

        listener?.onDisconnect()
    }

    fun start(url: String, language: String) {
        setLanguage(language)

        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = 3

        socket = IO.socket(url, options)

        socket?.on(Socket.EVENT_CONNECT, onConnect)
        socket?.on("operator_greet", onOperatorGreet)
        socket?.on("form_init", onFormInit)
        socket?.on("form_final", onFormFinal)
        socket?.on("feedback", onFeedback)
        socket?.on("user_queue", onUserQueue)
        socket?.on("operator_typing", onOperatorTyping)
        socket?.on("message", onMessage)
        socket?.on("category_list", onCategoryList)
        socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)

        socket?.connect()
    }

    fun sendCallInitialization(callInitialization: CallInitialization) {
        debug(TAG, "sendCallInitialization() -> $callInitialization")

        socket?.emit("initialize", jsonObject {
            when (callInitialization.callType) {
                CallType.TEXT -> {
                    // Ignored
                }
                CallType.AUDIO -> {
                    put("media", "audio")
                }
                CallType.VIDEO -> {
                    put("media", "video")
                }
            }

            putIfValueNotNull("user_id", callInitialization.userId)
            putIfValueNotNull("domain", callInitialization.domain)
            putIfValueNotNull("topic", callInitialization.topic)

            if (callInitialization.device != null) {
                put("device", jsonObject {
                    putIfValueNotNull("os", callInitialization.device.os)
                    putIfValueNotNull("os_ver", callInitialization.device.osVersion)
                    putIfValueNotNull("name", callInitialization.device.name)
                    putIfValueNotNull("mobile_operator", callInitialization.device.mobileOperator)
                    putIfValueNotNull("app_ver", callInitialization.device.appVersion)

                    if (callInitialization.device.battery != null) {
                        put("battery", jsonObject {
                            putIfValueNotNull("percentage", callInitialization.device.battery.percentage)
                            putIfValueNotNull("is_charging", callInitialization.device.battery.isCharging)
                            putIfValueNotNull("temperature", callInitialization.device.battery.temperature)
                        })
                    }
                })
            }

            if (callInitialization.authorization != null) {
                put("auth", jsonObject {
                    put("type", "bearer")
                    put("token", callInitialization.authorization.bearer.token)
                    put("refresh_token", callInitialization.authorization.bearer.refreshToken)
                })
            }

            if (callInitialization.location != null) {
                put("location", jsonObject {
                    put("lat", callInitialization.location.latitude)
                    put("lon", callInitialization.location.longitude)
                })
            }

            if (callInitialization.user != null) {
                put("user", jsonObject {
                    putIfValueNotNull("first_name", callInitialization.user.firstName)
                    putIfValueNotNull("last_name", callInitialization.user.lastName)
                    putIfValueNotNull("middle_name", callInitialization.user.middleName)
                    putIfValueNotNull("iin", callInitialization.user.iin)
                    putIfValueNotNull("phone_number", callInitialization.user.phoneNumber)
                    putIfValueNotNull("email", callInitialization.user.email)
                    putIfValueNotNull("birthDate", callInitialization.user.birthDate)
                })
            }

            put("lang", callInitialization.language.key)
        })
    }

    fun getBasicCategories(language: String? = null) {
        getCategories(0, language)
    }

    fun getCategories(parentId: Long, language: String? = null) {
//        debug(TAG, "requestCategories: $parentId")

        socket?.emit("user_dashboard", jsonObject {
            put("action", "get_category_list")
            put("parent_id", parentId)
            put("lang", fetchLanguage(language))
        })
    }

    fun getResponse(id: Int, language: String? = null) {
//        debug(TAG, "requestResponse: $id")

        socket?.emit("user_dashboard", jsonObject {
            put("action", "get_response")
            put("id", id)
            put("lang", fetchLanguage(language))
        })
    }

    fun sendFeedback(rating: Int, chatId: Long) {
        debug(TAG, "sendFeedback: $rating, $chatId")

        socket?.emit("user_feedback", jsonObject {
            put("r", rating)
            put("chat_id", chatId)
        })
    }

    fun sendUserMessage(message: String, language: String? = null) {
        debug(TAG, "sendUserMessage: $message")

        socket?.emit("user_message", jsonObject {
            put("text", message)
            put("lang", fetchLanguage(language))
        })
    }

    fun sendUserMediaMessage(mediaType: String, url: String) {
        socket?.emit("user_message", jsonObject {
            put(mediaType, url)
        })
    }

    fun sendMessage(
        rtc: RTC? = null,
        action: UserMessage.Action? = null,
        language: String? = null
    ): Emitter? {
        val userMessage = UserMessage(rtc, action).toJsonObject()
        userMessage.put("lang", fetchLanguage(language))

        debug(TAG, "sendMessage: $userMessage")

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
        setLanguage(language)

        socket?.emit("user_language", jsonObject {
            put("language", language)
        })
    }

    fun sendExternal(callbackData: String?) {
        debug(TAG, "sendExternal: $callbackData")

        socket?.emit("external", jsonObject {
            put("callback_data", callbackData)
        })
    }

    fun sendFormInit(formId: Long) {
        socket?.emit("form_init", jsonObject {
            put("form_id", formId)
        })
    }

    fun sendFormFinal(dynamicForm: DynamicForm) {
        debug(TAG, "sendFormFinal() -> dynamicForm: $dynamicForm")

        socket?.emit("form_final", jsonObject {
            put("form_id", dynamicForm.id)

            val nodes = JSONArray()
            val fields = JSONObject()

            dynamicForm.fields.forEach { field ->
                debug(TAG, "sendFormFinal() -> forEach: $field")

                if (field.isFlex) {
                    nodes.put(jsonObject { put(field.type.value, field.value ?: "") })
                } else {
                    if (!field.title.isNullOrBlank()) {
                        fields.put(field.title, jsonObject { put(field.type.value, field.value) })
                    }
                }
            }

            debug(TAG, "sendFormFinal() -> nodes: $nodes")
            debug(TAG, "sendFormFinal() -> fields: $fields")

            put("form_data", jsonObject {
                put("nodes", nodes)
                put("fields", fields)
            })
        })
    }

    fun sendCancel() {
        debug(TAG, "sendCancel")

        socket?.emit("cancel", jsonObject {
        })
    }

    fun cancelPendingCall() {
        debug(TAG, "cancelPendingCall")

        socket?.emit("cancel_pending_call")
    }

    fun release() {
//        socket?.off("call", onCall)
        socket?.off("operator_greet", onOperatorGreet)
        socket?.off("form_init", onFormInit)
        socket?.off("feedback", onFeedback)
        socket?.off("user_queue", onUserQueue)
        socket?.off("operator_typing", onOperatorTyping)
        socket?.off("message", onMessage)
        socket?.off("category_list", onCategoryList)
        socket?.disconnect()
        socket = null
    }

    private fun fetchLanguage(language: String?): String? {
        return if (!language.isNullOrBlank()) {
            language
        } else if (!this.language.isNullOrBlank()) {
            this.language
        } else {
            null
        }
    }

    interface Listener {
        fun onConnect()

        //        fun onCall(type: String, media: String, operator: String, instance: String)
        fun onOperatorGreet(fullName: String, photoUrl: String?, text: String)
        fun onFormInit(dynamicForm: DynamicForm)
        fun onFormFinal(text: String)
        fun onFeedback(text: String, ratingButtons: List<RatingButton>)
        fun onPendingUsersQueueCount(text: String? = null, count: Int)
        fun onNoOnlineOperators(text: String): Boolean
        fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean
        fun onNoResultsFound(text: String, timestamp: Long): Boolean
        fun onChatTimeout(text: String, timestamp: Long): Boolean
        fun onOperatorDisconnected(text: String, timestamp: Long): Boolean
        fun onUserRedirected(text: String, timestamp: Long): Boolean

        fun onCallAccept()
        fun onRTCPrepare()
        fun onRTCReady()
        fun onRTCAnswer(sessionDescription: SessionDescription)
        fun onRTCOffer(sessionDescription: SessionDescription)
        fun onRTCIceCandidate(iceCandidate: IceCandidate)
        fun onRTCHangup()

        fun onTextMessage(
            text: String?,
            replyMarkup: Message.ReplyMarkup? = null,
            attachments: List<Attachment>? = null,
            dynamicForm: DynamicForm? = null,
            timestamp: Long
        )
        fun onMediaMessage(
            media: Media,
            replyMarkup: Message.ReplyMarkup? = null,
            timestamp: Long
        )
        fun onEmptyMessage()

        fun onCategories(categories: List<Category>)

        fun onDisconnect()
    }

}