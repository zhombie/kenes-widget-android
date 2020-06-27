package q19.kenes_widget.network.http

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import q19.kenes_widget.model.Configs
import q19.kenes_widget.model.Configs.I18NString.Companion.parse
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.JsonUtil.parse
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.UrlUtil

internal class WidgetConfigsTask(private val url: String) : BaseTask<Configs> {

    override val TAG = "WidgetConfigsTask"

    override fun run(): Configs? {
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
//            val localBotConfigs = json.optJSONObject("local_bot_configs")

            val configs = Configs()

            configs.opponent = Configs.Opponent(
                name = configsJson?.optString("title"),
                secondName = configsJson?.optString("default_operator"),
                avatarUrl = UrlUtil.getStaticUrl(configsJson?.optString("image"))
            )

            if (contactsJson != null) {
                val contacts = mutableListOf<Configs.Contact>()

                for (key in contactsJson.keys()) {
                    val value = contactsJson[key]

                    if (value is String) {
                        contacts.add(Configs.Contact(key, value))
                    } else if (value is JSONArray) {
                        configs.phones = value.parse()
                    }
                }

                configs.contacts = contacts
            }

            configs.workingHours = Configs.WorkingHours(
                configsJson?.optString("message_kk"),
                configsJson?.optString("message_ru")
            )

            val infoBlocks = mutableListOf<Configs.InfoBlock>()
            if (infoBlocksJson != null) {
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
            configs.infoBlocks = infoBlocks

            if (booleansJson != null) {
                val keys = booleansJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = booleansJson.get(key)

                    debug(TAG, "key: $key, value: $value")

                    if (value is Boolean) {
                        when (key) {
                            "chatbot_enabled" ->
                                configs.isChabotEnabled = value
                            "audio_call_enabled" ->
                                configs.isAudioCallEnabled = value
                            "video_call_enabled" ->
                                configs.isVideoCallEnabled = value
                            "contact_sections_shown" ->
                                configs.isContactSectionsShown = value
                        }
                    }
                }
            }

            return configs
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(TAG, "ERROR! $e")
            return null
        }
    }

}