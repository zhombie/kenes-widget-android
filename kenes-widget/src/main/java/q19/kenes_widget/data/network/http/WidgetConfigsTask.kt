package q19.kenes_widget.data.network.http

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import q19.kenes_widget.data.model.Configs
import q19.kenes_widget.data.model.Configs.I18NString.Companion.parse
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.parse
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.UrlUtil
import q19.kenes_widget.util.findEnumBy

internal class WidgetConfigsTask(private val url: String) : BaseTask<Configs> {

    override val TAG = "WidgetConfigsTask"

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
            val infoBlocksJson = json?.optJSONArray("info_blocks")
            val booleansJson = json?.optJSONObject("booleans")
            val callScopesJson = json?.optJSONArray("call_scopes")
//            val localBotConfigs = json.optJSONObject("local_bot_configs")

            debug(TAG, "callScopesJson: $callScopesJson")

            val opponent = Configs.Opponent(
                name = configsJson?.optString("title"),
                secondName = configsJson?.optString("default_operator"),
                avatarUrl = UrlUtil.getStaticUrl(configsJson?.optString("image"))
            )

            var contacts: MutableList<Configs.Contact>? = null
            var phones: List<String>? = null
            if (contactsJson != null) {
                contacts = mutableListOf()
                phones = listOf()
                for (key in contactsJson.keys()) {
                    val value = contactsJson[key]

                    if (value is String) {
                        contacts.add(Configs.Contact(key, value))
                    } else if (value is JSONArray) {
                        phones = value.parse()
                    }
                }
            }

            val workingHours = Configs.WorkingHours(
                configsJson?.optString("message_kk"),
                configsJson?.optString("message_ru")
            )

            var infoBlocks: MutableList<Configs.InfoBlock>? = null
            if (infoBlocksJson != null) {
                infoBlocks = mutableListOf()
                for (i in 0 until infoBlocksJson.length()) {
                    val infoBlock = infoBlocksJson[i] as JSONObject

                    val items = mutableListOf<Configs.Item>()
                    val itemsJson = infoBlock.getJSONArray("items")
                    if (itemsJson.length() > 0) {
                        for (j in 0 until itemsJson.length()) {
                            val item = itemsJson.get(j) as JSONObject
                            items.add(Configs.Item(
                                icon = UrlUtil.getStaticUrl(item.getNullableString("icon")),
                                text = item.getString("text"),
                                description = item.getJSONObject("description").parse(),
                                action = item.getString("action")
                            ))
                        }
                    }

                    infoBlocks.add(
                        Configs.InfoBlock(
                            title = infoBlock.getJSONObject("title").parse(),
                            description = infoBlock.getJSONObject("description").parse(),
                            items = items
                        )
                    )
                }
            }

            var isChabotEnabled = false
            var isAudioCallEnabled = false
            var isVideoCallEnabled = false
            var isContactSectionsShown = false
            var isPhonesListShown = false
            var isOperatorsScoped = false
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
                                isChabotEnabled = value
                            "audio_call_enabled" ->
                                isAudioCallEnabled = value
                            "video_call_enabled" ->
                                isVideoCallEnabled = value
                            "contact_sections_shown" ->
                                isContactSectionsShown = value
                            "phones_list_shown" ->
                                isPhonesListShown = value
                            "operators_scoped" ->
                                isOperatorsScoped = value
                            "services_enabled" ->
                                isServicesEnabled = value
                        }
                    }
                }
            }
            val booleans = Configs.Booleans(
                isChabotEnabled = isChabotEnabled,
                isAudioCallEnabled = isAudioCallEnabled,
                isVideoCallEnabled = isVideoCallEnabled,
                isContactSectionsShown = isContactSectionsShown,
                isPhonesListShown = isPhonesListShown,
                isOperatorsScoped = isOperatorsScoped,
                isServicesEnabled = isServicesEnabled
            )

            val callScopes = mutableListOf<Configs.CallScope>()
            if (callScopesJson != null) {
                for (i in 0 until callScopesJson.length()) {
                    val callScope = callScopesJson[i] as JSONObject

                    callScopes.add(
                        Configs.CallScope(
                            id = callScope.getLong("id"),
                            type = findEnumBy { it.value == callScope.getNullableString("type") },
                            scope = callScope.getString("scope"),
                            title = callScope.getJSONObject("title").parse(),
                            parentId = callScope.getLong("parent_id"),
                            chatType = findEnumBy { it.value == callScope.getNullableString("chat_type") },
                            action = findEnumBy { it.value == callScope.getNullableString("action") },
                            details = Configs.CallScope.Details(callScope.optInt("order"))
                        )
                    )
                }
            }

            return Configs(
                booleans = booleans,
                opponent = opponent,
                contacts = contacts,
                phones = phones,
                workingHours = workingHours,
                infoBlocks = infoBlocks,
                callScopes = callScopes.sortedBy { it.details?.order }
            )
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(TAG, "ERROR! $e")
            return null
        }
    }

}