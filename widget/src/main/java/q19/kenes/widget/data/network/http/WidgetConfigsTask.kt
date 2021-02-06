package q19.kenes.widget.data.network.http

import android.util.Log
import kz.q19.data.api.model.response.configs.ConfigsResponse
import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.i18n.I18NString
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getObjectOrNull
import kz.q19.utils.json.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject
import q19.kenes.widget.util.Logger.debug
import q19.kenes.widget.util.UrlUtil

internal class WidgetConfigsTask constructor(private val url: String) : BaseTask<Configs> {

    override val TAG: String = WidgetConfigsTask::class.java.simpleName

    override fun execute(): Configs? {
        try {
            val asyncTask = HttpRequestHandler(url = url)
            val response = asyncTask.execute().get()

            val json = if (response.isNullOrBlank()) {
                null
            } else {
                JSONObject(response)
            }

            val configsJson = json?.optJSONObject("configs")
            val contactsJson = json?.optJSONObject("contacts")
//            val infoBlocksJson = json?.optJSONArray("info_blocks")
            val booleansJson = json?.optJSONObject("booleans")
            val callScopesJsonArray = json?.optJSONArray("call_scopes")
//            val localBotConfigs = json.optJSONObject("local_bot_configs")

            debug(TAG, "callScopesJson: $callScopesJsonArray")

            val bot = Configs.Bot(
                image = UrlUtil.getStaticUrl(configsJson?.optString("image")),
                title = configsJson?.optString("title")
            )

            val callAgent = Configs.CallAgent(
                defaultName = configsJson?.optString("default_operator")
            )

            var socials: List<Configs.Contacts.Social>? = null
            var phoneNumbers: List<Configs.Contacts.PhoneNumber>? = null
            if (contactsJson != null) {
                socials = mutableListOf()
                phoneNumbers = mutableListOf()
                for (key in contactsJson.keys()) {
                    val value = contactsJson[key]

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

            var isChatBotEnabled = false
            var isAudioCallEnabled = false
            var isVideoCallEnabled = false
            var isContactSectionsShown = false
            var isPhonesListShown = false
            var isCallAgentsScoped = false
            var isServicesEnabled = false
            if (booleansJson != null) {
                val keys = booleansJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = booleansJson.get(key)

                    debug(TAG, "key: $key, value: $value")

                    if (value is Boolean) {
                        when (key) {
                            "chatbot_enabled" ->
                                isChatBotEnabled = value
                            "audio_call_enabled" ->
                                isAudioCallEnabled = value
                            "video_call_enabled" ->
                                isVideoCallEnabled = value
                            "contact_sections_shown" ->
                                isContactSectionsShown = value
                            "phones_list_shown" ->
                                isPhonesListShown = value
                            "operators_scoped" ->
                                isCallAgentsScoped = value
                            "services_enabled" ->
                                isServicesEnabled = value
                        }
                    }
                }
            }
            val preferences = Configs.Preferences(
                isChatBotEnabled = isChatBotEnabled,
                isAudioCallEnabled = isAudioCallEnabled,
                isVideoCallEnabled = isVideoCallEnabled,
                isContactSectionsShown = isContactSectionsShown,
                isPhonesListShown = isPhonesListShown,
                isCallAgentsScoped = isCallAgentsScoped,
                isServicesEnabled = isServicesEnabled
            )

            val calls = mutableListOf<Configs.Call>()
            val services = mutableListOf<Configs.Service>()
            val forms = mutableListOf<Configs.Form>()
            if (callScopesJsonArray != null) {
                for (i in 0 until callScopesJsonArray.length()) {
                    val callScopeJsonObject = callScopesJsonArray[i]

                    if (callScopeJsonObject is JSONObject) {
                        val id = callScopeJsonObject.getLongOrNull("id") ?: continue

                        val parentId = callScopeJsonObject.getLongOrNull("parent_id") ?: ConfigsResponse.CallScopeResponse.NO_PARENT_ID

                        val type = when (callScopeJsonObject.getStringOrNull("type")) {
                            ConfigsResponse.CallScopeResponse.TypeResponse.FOLDER.value -> Configs.Nestable.Type.FOLDER
                            ConfigsResponse.CallScopeResponse.TypeResponse.LINK.value -> Configs.Nestable.Type.LINK
                            else -> null
                        }

                        var title: I18NString? = null
                        val titleJsonObject = callScopeJsonObject.getObjectOrNull("title")
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

                        val detailsJsonObject = callScopeJsonObject.getObjectOrNull("details")

                        val extra = Configs.Nestable.Extra(
                            order = detailsJsonObject?.getInt("order"),
                        )

                        when (callScopeJsonObject.getStringOrNull("chat_type")) {
                            ConfigsResponse.CallScopeResponse.ChatTypeResponse.AUDIO.value,
                            ConfigsResponse.CallScopeResponse.ChatTypeResponse.VIDEO.value -> {
                                val callType = when (callScopeJsonObject.getStringOrNull("action")) {
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
                                        scope = callScopeJsonObject.getStringOrNull("scope"),
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
                                            formId = detailsJsonObject.getLong("form_id"),
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

            return Configs(
                bot = bot,
                callAgent = callAgent,
                preferences = preferences,
                contacts = Configs.Contacts(phoneNumbers, socials),
                calls = calls,
                forms = forms,
                services = services
            )
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(TAG, "ERROR! $e")
            return null
        }
    }

}