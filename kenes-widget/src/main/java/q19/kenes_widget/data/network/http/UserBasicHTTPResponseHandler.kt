package q19.kenes_widget.data.network.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import q19.kenes_widget.data.model.IDP
import q19.kenes_widget.util.Logger

internal class UserBasicHTTPResponseHandler constructor(
    private val onSuccess: (person: IDP.Person) -> Unit,
    private val onError: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = UserBasicHTTPResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        Logger.debug(TAG, "onSuccess() -> response: $response")
        val jsonObject = response ?: return
        val personJSONObject = jsonObject.optJSONObject("person") ?: return
        onSuccess(
            IDP.Person(
                iin = personJSONObject.optString("iin"),
                surname = personJSONObject.optString("surname"),
                name = personJSONObject.optString("name"),
                patronymic = personJSONObject.optString("patronymic"),
                birthDate = personJSONObject.optString("birthDate"),
            )
        )
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONObject?
    ) {
        Logger.debug(TAG, "onFailure() -> errorResponse: $errorResponse, throwable: $throwable")
        onError(throwable)
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        responseString: String?,
        throwable: Throwable?
    ) {
        Logger.debug(TAG, "onFailure() -> throwable: $throwable")
        onError(throwable)
    }

}