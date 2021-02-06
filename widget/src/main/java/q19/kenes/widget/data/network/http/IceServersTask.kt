package q19.kenes.widget.data.network.http

import android.util.Log
import kz.q19.domain.model.webrtc.IceServer
import org.json.JSONObject

internal class IceServersTask constructor(
    private val url: String
) : BaseTask<List<IceServer>> {

    override val TAG = "IceServersTask"

    override fun execute(): List<IceServer>? {
        try {
            val asyncTask = HttpRequestHandler(url = url)
            val response = asyncTask.execute().get()

            val json = if (response.isNullOrBlank()) {
                null
            } else {
                JSONObject(response)
            }

            val iceServersJson = json?.optJSONArray("ice_servers")

            val iceServers = mutableListOf<IceServer>()

            if (iceServersJson != null) {
                for (i in 0 until iceServersJson.length()) {
                    val iceServerJson = iceServersJson[i] as? JSONObject?

                    iceServers.add(
                        IceServer(
                            url = iceServerJson?.optString("url") ?: "",
                            urls = iceServerJson?.optString("urls") ?: "",
                            username = iceServerJson?.optString("username"),
                            credential = iceServerJson?.optString("credential")
                        )
                    )
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