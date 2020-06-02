package q19.kenes_widget

import io.ktor.client.HttpClient
import q19.kenes_widget.core.ktor.initKtorClient

object InjectionProvider {

    fun initKtor(): HttpClient {
        return initKtorClient()
    }

}