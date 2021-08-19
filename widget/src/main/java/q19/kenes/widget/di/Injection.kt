package q19.kenes.widget.di

import android.content.Context
import kz.q19.common.locale.LocaleManager
import kz.q19.domain.model.language.Language
import kz.q19.socket.SocketClient
import kz.q19.socket.SocketClientConfig
import kz.q19.socket.repository.SocketRepository
import kz.q19.webrtc.PeerConnectionClient
import q19.kenes.widget.core.device.DeviceInfo
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.KenesWidgetPresenter
import q19.kenes.widget.ui.presentation.call.Call
import q19.kenes.widget.ui.presentation.call.CallsPresenter
import q19.kenes.widget.ui.presentation.call.text.TextChatPresenter
import q19.kenes.widget.ui.presentation.call.video.VideoCallPresenter
import q19.kenes.widget.ui.presentation.home.ChatbotPresenter

internal class Injection private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: Injection? = null

        fun getInstance(context: Context): Injection =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Injection(context).also { INSTANCE = it }
            }
    }

    private val database: Database by lazy { Database.getInstance(context) }

    private val deviceInfo: DeviceInfo by lazy { DeviceInfo(context) }

    private val socketRepository: SocketRepository by lazy {
        val language = Language.from(LocaleManager.getLocale() ?: Language.DEFAULT.locale)
        SocketClientConfig.init(true, language)
        SocketClient.getInstance()
    }

    fun provideKenesWidgetPresenter(language: Language): KenesWidgetPresenter {
        return KenesWidgetPresenter(language, database, socketRepository)
    }

    fun provideChatbotPresenter(language: Language): ChatbotPresenter {
        return ChatbotPresenter(language, database, socketRepository)
    }

    fun provideCallsPresenter(language: Language): CallsPresenter {
        return CallsPresenter(language, database, deviceInfo, socketRepository)
    }

    fun provideVideoCallPresenter(
        language: Language,
        call: Call,
        peerConnectionClient: PeerConnectionClient
    ): VideoCallPresenter {
        return VideoCallPresenter(
            language,
            call,
            database,
            deviceInfo,
            peerConnectionClient,
            socketRepository
        )
    }

    fun provideTextChatPresenter(language: Language): TextChatPresenter {
        return TextChatPresenter(language, socketRepository)
    }

    fun destroy() {
        socketRepository.removeAllListeners()
        socketRepository.release()

        database.destroy()

        INSTANCE = null
    }

}