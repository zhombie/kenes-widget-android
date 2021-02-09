package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.domain.model.webrtc.IceServer
import kz.q19.utils.json.getStringOrNull
import org.json.JSONObject

internal class IceServersResponseHandler constructor(
    private val onSuccess: (configs: List<IceServer>) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = IceServersResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val json = response ?: return

        val iceServersJSONArray = json.optJSONArray("ice_servers")

        val iceServers = mutableListOf<IceServer>()

        if (iceServersJSONArray != null) {
            for (i in 0 until iceServersJSONArray.length()) {
                val iceServerJSONObject = iceServersJSONArray[i]
                if (iceServerJSONObject is JSONObject) {
                    iceServers.add(
                        IceServer(
                            url = iceServerJSONObject.getStringOrNull("url") ?: "",
                            urls = iceServerJSONObject.getStringOrNull("urls") ?: "",
                            username = iceServerJSONObject.getStringOrNull("username"),
                            credential = iceServerJSONObject.getStringOrNull("credential")
                        )
                    )
                }
            }
        }

        onSuccess(iceServers)
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