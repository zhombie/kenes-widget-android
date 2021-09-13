package kz.q19.kenes.widget.data.remote.file

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestHandle
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONObject

internal fun AsyncHttpClient.uploadFile(
    url: String?,
    params: RequestParams,
    onSuccess: (path: String, hash: String) -> Unit,
    onFailure: (throwable: Throwable?) -> Unit = {}
): RequestHandle? {
    if (url.isNullOrBlank()) {
        onFailure(NullPointerException())
        return null
    }

    return post(url, params, object : JsonHttpResponseHandler() {
        override fun onSuccess(
            statusCode: Int,
            headers: Array<out Header>?,
            response: JSONObject?
        ) {
            super.onSuccess(statusCode, headers, response)

            val hash = response?.optString("hash")
            val path = response?.optString("url")

            if (hash.isNullOrBlank() || path.isNullOrBlank()) {
                onFailure(NullPointerException())
            } else {
                onSuccess(path, hash)
            }
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            errorResponse: JSONObject?
        ) {
            onFailure(throwable)
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            errorResponse: JSONArray?
        ) {
            onFailure(throwable)
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            responseString: String?,
            throwable: Throwable?
        ) {
            onFailure(throwable)
        }
    })
}