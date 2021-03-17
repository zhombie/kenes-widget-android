package q19.kenes_widget.data.network.http

import com.loopj.android.http.AsyncHttpClient

object AsyncHTTPClient {

    private var asyncHttpClient: AsyncHttpClient? = null

    fun get(headers: List<Pair<String, String>>? = null): AsyncHttpClient {
        if (asyncHttpClient == null) {
            asyncHttpClient = AsyncHttpClient()
            asyncHttpClient?.isLoggingEnabled = false
            asyncHttpClient?.connectTimeout = 30 * 1000
            asyncHttpClient?.responseTimeout = 30 * 1000
            headers?.forEach {
                asyncHttpClient?.addHeader(it.first, it.second)
            }
        }
        return requireNotNull(asyncHttpClient)
    }

    fun new(headers: List<Pair<String, String>>? = null): AsyncHttpClient {
        val asyncHttpClient = AsyncHttpClient()
        asyncHttpClient.isLoggingEnabled = false
        asyncHttpClient.connectTimeout = 30 * 1000
        asyncHttpClient.responseTimeout = 30 * 1000
        headers?.forEach {
            asyncHttpClient.addHeader(it.first, it.second)
        }
        return asyncHttpClient
    }

}