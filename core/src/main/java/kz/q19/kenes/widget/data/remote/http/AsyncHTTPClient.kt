package kz.q19.kenes.widget.data.remote.http

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestHandle
import com.loopj.android.http.RequestParams
import com.loopj.android.http.ResponseHandlerInterface

internal class AsyncHTTPClient private constructor(
    private val asyncHttpClient: AsyncHttpClient
) {

    class Builder {
        private var isLoggingEnabled: Boolean = false

        fun isLoggingEnabled(isEnabled: Boolean) {
            this.isLoggingEnabled = isEnabled
        }

        fun build(): AsyncHTTPClient {
            val asyncHttpClient = AsyncHttpClient()
            asyncHttpClient.isLoggingEnabled = isLoggingEnabled
            asyncHttpClient.connectTimeout = 30 * 1000
            asyncHttpClient.responseTimeout = 30 * 1000
            return AsyncHTTPClient(asyncHttpClient)
        }
    }

    fun get(url: String?, responseHandler: ResponseHandlerInterface): RequestHandle? {
        if (url.isNullOrBlank()) return null
        return asyncHttpClient.get(url, responseHandler)
    }

    fun get(url: String?, params: RequestParams, responseHandler: ResponseHandlerInterface): RequestHandle? {
        if (url.isNullOrBlank()) return null
        return asyncHttpClient.get(url, params, responseHandler)
    }

    fun post(url: String?, responseHandler: ResponseHandlerInterface): RequestHandle? {
        if (url.isNullOrBlank()) return null
        return asyncHttpClient.post(url, responseHandler)
    }

    fun post(url: String?, params: RequestParams, responseHandler: ResponseHandlerInterface): RequestHandle? {
        if (url.isNullOrBlank()) return null
        return asyncHttpClient.post(url, params, responseHandler)
    }

    fun dispose() {
        asyncHttpClient.cancelAllRequests(true)
    }

}