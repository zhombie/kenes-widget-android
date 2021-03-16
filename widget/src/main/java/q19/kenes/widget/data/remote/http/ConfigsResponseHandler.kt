package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.data.api.model.response.configs.ConfigsResponse
import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.i18n.I18NId
import kz.q19.domain.model.i18n.I18NString
import kz.q19.utils.json.*
import org.json.JSONArray
import org.json.JSONObject
import q19.kenes.widget.util.UrlUtil

internal open class ConfigsResponseHandler constructor(
    private val onSuccess: (configs: Configs) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG: String = ConfigsResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val jsonObject = response ?: return

        val configsJSONObject = jsonObject.getJSONObjectOrNull("configs")
        val contactsJSONObject = jsonObject.getJSONObjectOrNull("contacts")
//        val infoBlocksJSONArray = json?.optJSONArray("info_blocks")
        val booleansJSONObject = jsonObject.getJSONObjectOrNull("booleans")
        val callScopesJSONArray = jsonObject.getJSONArrayOrNull("call_scopes")
//        val localBotConfigsJSONObject = json.optJSONObject("local_bot_configs")

//        Logger.debug(TAG, "callScopesJSONArray: $callScopesJSONArray")

        val bot = Configs.Bot(
            image = UrlUtil.getStaticUrl(configsJSONObject?.getStringOrNull("image")),
            title = configsJSONObject?.getStringOrNull("title")
        )

        val callAgent = Configs.CallAgent(
            defaultName = configsJSONObject?.getStringOrNull("default_operator")
        )

        var socials: List<Configs.Contacts.Social>? = null
        var phoneNumbers: List<Configs.Contacts.PhoneNumber>? = null
        if (contactsJSONObject != null) {
            socials = mutableListOf()
            phoneNumbers = mutableListOf()
            for (key in contactsJSONObject.keys()) {
                val value = contactsJSONObject[key]

                if (value is String) {
                    val socialId = when (key) {
                        Configs.Contacts.Social.Id.FACEBOOK.id ->
                            Configs.Contacts.Social.Id.FACEBOOK
                        Configs.Contacts.Social.Id.TELEGRAM.id ->
                            Configs.Contacts.Social.Id.TELEGRAM
                        Configs.Contacts.Social.Id.TWITTER.id ->
                            Configs.Contacts.Social.Id.TWITTER
                        Configs.Contacts.Social.Id.VK.id ->
                            Configs.Contacts.Social.Id.VK
                        else -> continue
                    }
                    socials.add(Configs.Contacts.Social(socialId, value))
                } else if (value is JSONArray) {
                    for (i in 0 until value.length()) {
                        val phoneNumber = value[i]
                        if (phoneNumber is String) {
                            phoneNumbers.add(Configs.Contacts.PhoneNumber(phoneNumber))
                        }
                    }
                }
            }
        }

        var preferences = Configs.Preferences(
            isChatBotEnabled = false,
            isPhonesListShown = false,
            isContactSectionsShown = false,
            isAudioCallEnabled = false,
            isVideoCallEnabled = false,
            isServicesEnabled = false,
            isCallAgentsScoped = false
        )
        if (booleansJSONObject != null) {
            val keys = booleansJSONObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = booleansJSONObject.get(key)

//                Logger.debug(TAG, "key: $key, value: $value")

                if (value is Boolean) {
                    when (key) {
                        "chatbot_enabled" ->
                            preferences = preferences.copy(isChatBotEnabled = value)
                        "audio_call_enabled" ->
                            preferences = preferences.copy(isAudioCallEnabled = value)
                        "video_call_enabled" ->
                            preferences = preferences.copy(isVideoCallEnabled = value)
                        "contact_sections_shown" ->
                            preferences = preferences.copy(isContactSectionsShown = value)
                        "phones_list_shown" ->
                            preferences = preferences.copy(isPhonesListShown = value)
                        "operators_scoped" ->
                            preferences = preferences.copy(isCallAgentsScoped = value)
                        "services_enabled" ->
                            preferences = preferences.copy(isServicesEnabled = value)
                    }
                }
            }
        }

        val calls = mutableListOf<Configs.Call>()
        val services = mutableListOf<Configs.Service>()
        val forms = mutableListOf<Configs.Form>()
        if (callScopesJSONArray != null) {
            for (i in 0 until callScopesJSONArray.length()) {
                val callScopeJSONObject = callScopesJSONArray[i]

                if (callScopeJSONObject is JSONObject) {
                    val id = callScopeJSONObject.getLongOrNull("id") ?: continue

                    val parentId = callScopeJSONObject.getLongOrNull("parent_id")
                        ?: ConfigsResponse.CallScopeResponse.NO_PARENT_ID

                    val type = when (callScopeJSONObject.getStringOrNull("type")) {
                        ConfigsResponse.CallScopeResponse.TypeResponse.FOLDER.value -> Configs.Nestable.Type.FOLDER
                        ConfigsResponse.CallScopeResponse.TypeResponse.LINK.value -> Configs.Nestable.Type.LINK
                        else -> null
                    }

                    var title: I18NString? = null
                    val titleJsonObject = callScopeJSONObject.getJSONObjectOrNull("title")
                    if (titleJsonObject != null) {
                        val kk = if (!titleJsonObject.getStringOrNull("kk").isNullOrBlank()) {
                            titleJsonObject.getStringOrNull("kk")
                        } else {
                            titleJsonObject.getStringOrNull("kz")
                        }

                        title = I18NString(
                            kk = kk,
                            ru = titleJsonObject.getStringOrNull("ru"),
                            en = titleJsonObject.getStringOrNull("en")
                        )
                    }
                    if (title == null) {
                        continue
                    }

                    val detailsJsonObject = callScopeJSONObject.getJSONObjectOrNull("details")

                    val extra = Configs.Nestable.Extra(
                        order = detailsJsonObject?.getIntOrNull("order"),
                    )

                    when (callScopeJSONObject.getStringOrNull("chat_type")) {
                        ConfigsResponse.CallScopeResponse.ChatTypeResponse.AUDIO.value,
                        ConfigsResponse.CallScopeResponse.ChatTypeResponse.VIDEO.value -> {
                            val callType = when (callScopeJSONObject.getStringOrNull("action")) {
                                ConfigsResponse.CallScopeResponse.ActionResponse.AUDIO_CALL.value ->
                                    CallType.AUDIO
                                ConfigsResponse.CallScopeResponse.ActionResponse.VIDEO_CALL.value ->
                                    CallType.VIDEO
                                else -> null
                            }
                            calls.add(
                                Configs.Call(
                                    id = id,
                                    parentId = parentId,
                                    type = type,
                                    callType = callType,
                                    scope = callScopeJSONObject.getStringOrNull("scope"),
                                    title = title,
                                    extra = extra
                                )
                            )
                        }
                        ConfigsResponse.CallScopeResponse.ChatTypeResponse.EXTERNAL.value,
                        ConfigsResponse.CallScopeResponse.ChatTypeResponse.FORM.value -> {
                            if (detailsJsonObject?.getLongOrNull("form_id") != null) {
                                forms.add(
                                    Configs.Form(
                                        id = id,
                                        parentId = parentId,
                                        type = type,
                                        formId = I18NId(
                                            kk = detailsJsonObject.getJSONObjectOrNull("form")?.getLongOrNull("kk"),
                                            ru = detailsJsonObject.getJSONObjectOrNull("form")?.getLongOrNull("ru"),
                                            en = detailsJsonObject.getJSONObjectOrNull("form")?.getLongOrNull("en")
                                        ),
                                        title = title,
                                        extra = extra
                                    )
                                )
                            } else if (detailsJsonObject?.getLongOrNull("external_id") != null) {
                                services.add(
                                    Configs.Service(
                                        id = id,
                                        parentId = parentId,
                                        type = type,
                                        serviceId = detailsJsonObject.getLong("external_id"),
                                        title = title,
                                        extra = extra
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        onSuccess(
            Configs(
                bot = bot,
                callAgent = callAgent,
                preferences = preferences,
                contacts = Configs.Contacts(phoneNumbers, socials),
                calls = calls,
                forms = forms,
                services = services
            )
        )
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONObject?
    ) {
        onFailure(throwable)
    }

}