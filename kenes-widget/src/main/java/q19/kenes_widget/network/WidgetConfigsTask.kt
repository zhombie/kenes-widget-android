package q19.kenes_widget.network

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

            val configs = json?.optJSONObject("configs")
            val contacts = json?.optJSONObject("contacts")
//            val localBotConfigs = json.optJSONObject("local_bot_configs")

            val data = Configs()

            data.opponent = Configs.Opponent(
                name = configs?.optString("default_operator"),
                secondName = configs?.optString("title"),
                avatarUrl = UrlUtil.getStaticUrl(configs?.optString("image"))
            )

            contacts?.keys()?.forEach { key ->
                val value = contacts[key]

                if (value is String) {
                    data.contacts.add(Configs.Contact(key, value))
                } else if (value is JSONArray) {
                    data.phones = value.parse()
                }
            }

            data.workingHours = Configs.WorkingHours(
                configs?.optString("message_kk"),
                configs?.optString("message_ru")
            )

            return data
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(tag, "ERROR! $e")
            return null
        }
    }

}