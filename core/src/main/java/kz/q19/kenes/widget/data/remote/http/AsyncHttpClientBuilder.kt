package kz.q19.kenes.widget.data.remote.http

import com.loopj.android.http.AsyncHttpClient
import kz.q19.kenes.widget.BuildConfig

internal class AsyncHttpClientBuilder {

    private var asyncHttpClient: AsyncHttpClient? = null

    init {
        asyncHttpClient = AsyncHttpClient()
        asyncHttpClient?.isLoggingEnabled = BuildConfig.DEBUG
        asyncHttpClient?.connectTimeout = 30 * 1000
        asyncHttpClient?.responseTimeout = 30 * 1000
    }

    fun build(): AsyncHttpClient? {
        return asyncHttpClient
    }

}