package q19.kenes_widget.core.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.logging.ANDROID
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging

fun initKtorClient() = HttpClient(Android) {
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.ALL
    }
}