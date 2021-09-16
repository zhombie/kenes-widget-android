package kz.q19.kenes.widget.data.remote.file

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.loopj.android.http.RequestHandle
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONObject

internal fun AsyncHttpClient.upload(
    url: String?,
    params: RequestParams,
    listener: (result: UploadResult) -> Unit
): RequestHandle? {
    if (url.isNullOrBlank()) {
        listener(UploadResult.Error(cause = NullPointerException()))
        return null
    }

    return post(url, params, object : JsonHttpResponseHandler() {
        override fun onProgress(bytesWritten: Long, totalSize: Long) {
            val progress = (100 * bytesWritten / totalSize).toInt()
            listener(UploadResult.Progress(progress))
        }

        override fun onSuccess(
            statusCode: Int,
            headers: Array<out Header>?,
            response: JSONObject?
        ) {
            super.onSuccess(statusCode, headers, response)

            val hash = response?.optString("hash")
            val title = response?.optString("title")
            val urlPath = response?.optString("url")

            if (hash.isNullOrBlank() || urlPath.isNullOrBlank()) {
                listener(UploadResult.Error(cause = NullPointerException()))
            } else {
                listener(UploadResult.Success(hash = hash, title = title, urlPath = urlPath))
            }
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            errorResponse: JSONObject?
        ) {
            listener(UploadResult.Error(cause = throwable))
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            errorResponse: JSONArray?
        ) {
            listener(UploadResult.Error(cause = throwable))
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            responseString: String?,
            throwable: Throwable?
        ) {
            listener(UploadResult.Error(cause = throwable))
        }
    })
}