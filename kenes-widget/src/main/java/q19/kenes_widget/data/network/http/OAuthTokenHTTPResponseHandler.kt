package q19.kenes_widget.data.network.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONObject
import q19.kenes_widget.data.model.OAuth
import q19.kenes_widget.util.JsonUtil.getNullableLong
import q19.kenes_widget.util.JsonUtil.getNullableString
import q19.kenes_widget.util.Logger

class OAuthTokenHTTPResponseHandler constructor(
    private val onSuccess: (oauth: OAuth) -> Unit,
    private val onError: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = OAuthTokenHTTPResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        Logger.debug(TAG, "onSuccess() -> response: $response")
        val jsonObject = response ?: return
        onSuccess(
            OAuth(
                accessToken = jsonObject.optString("access_token"),
                tokenType = jsonObject.optString("token_type"),
                refreshToken = jsonObject.getNullableString("refresh_token"),
                expiresIn = jsonObject.getNullableLong("expires_in"),
                scope = jsonObject.getNullableString("scope")
            )
        )
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONObject?
    ) {
        Logger.debug(TAG, "errorResponse: $errorResponse")
        onError(throwable)
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        responseString: String?,
        throwable: Throwable?
    ) {
        Logger.debug(TAG, "responseString: $responseString")
        onError(throwable)
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONArray?
    ) {
        Logger.debug(TAG, "errorResponse: $errorResponse")
        onError(throwable)
    }

}