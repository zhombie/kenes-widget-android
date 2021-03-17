package q19.kenes_widget.data.network.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject

internal class UserPhoneHTTPResponseHandler constructor(
    private val onSuccess: (phoneNumber: String) -> Unit,
    private val onError: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val jsonObject = response ?: return
        onSuccess(jsonObject.optString("phone"))
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        responseString: String?,
        throwable: Throwable?
    ) {
        onError(throwable)
    }

}