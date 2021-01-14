package q19.kenes_widget.data.network.file

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestHandle
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import org.json.JSONObject

internal fun AsyncHttpClient.uploadFile(
    url: String,
    params: RequestParams,
    listener: (path: String, hash: String) -> Unit
): RequestHandle? {
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
                return
            }

            listener(path, hash)
        }
    })
}