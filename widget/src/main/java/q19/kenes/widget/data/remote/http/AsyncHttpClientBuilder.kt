package q19.kenes.widget.data.remote.http

import com.loopj.android.http.AsyncHttpClient

object AsyncHttpClientBuilder {

    private var asyncHttpClient: AsyncHttpClient? = null

    init {
        asyncHttpClient = AsyncHttpClient()
        asyncHttpClient?.isLoggingEnabled = false
        asyncHttpClient?.connectTimeout = 30 * 1000
        asyncHttpClient?.responseTimeout = 30 * 1000
    }

    fun build(): AsyncHttpClient? {
        return asyncHttpClient
    }

}