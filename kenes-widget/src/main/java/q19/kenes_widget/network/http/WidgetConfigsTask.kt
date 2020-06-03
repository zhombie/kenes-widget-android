package q19.kenes_widget.network.http

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import q19.kenes_widget.model.Configs
import q19.kenes_widget.util.JsonUtil.parse
import q19.kenes_widget.util.UrlUtil

internal class WidgetConfigsTask(private val url: String) : BaseTask<Configs> {

    override val tag: String
        get() = "WidgetConfigsTask"

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
//            val localBotConfigs = json.optJSONObject("local_bot_configs")

            val configs = Configs()

            configs.opponent = Configs.Opponent(
                name = configsJson?.optString("default_operator"),
                secondName = configsJson?.optString("title"),
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

            return configs
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(tag, "ERROR! $e")
            return null
        }
    }

}