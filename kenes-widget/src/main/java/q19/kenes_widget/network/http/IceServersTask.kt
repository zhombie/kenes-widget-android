package q19.kenes_widget.network.http

import android.util.Log
import org.json.JSONObject
import q19.kenes_widget.model.WidgetIceServer

internal class IceServersTask(private val url: String) : BaseTask<List<WidgetIceServer>> {

    override val TAG = "IceServersTask"

    override fun run(): List<WidgetIceServer>? {
        try {
            val asyncTask = HttpRequestHandler(url = url)
            val response = asyncTask.execute().get()

            val json = if (response.isNullOrBlank()) {
                null
            } else {
                JSONObject(response)
            }

            val iceServersJson = json?.optJSONArray("ice_servers")

            val iceServers = mutableListOf<WidgetIceServer>()

            if (iceServersJson != null) {
                for (i in 0 until iceServersJson.length()) {
                    val iceServerJson = iceServersJson[i] as? JSONObject?

                    iceServers.add(WidgetIceServer(
                        url = iceServerJson?.optString("url"),
                        username = iceServerJson?.optString("username"),
                        urls = iceServerJson?.optString("urls"),
                        credential = iceServerJson?.optString("credential")
                    ))
                }
            }

            return iceServers
        } catch (e: Exception) {
//            e.printStackTrace()
            Log.e(TAG, "ERROR! $e")
            return null
        }
    }

}