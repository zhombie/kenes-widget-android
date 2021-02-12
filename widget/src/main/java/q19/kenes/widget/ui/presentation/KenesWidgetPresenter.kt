package q19.kenes.widget.ui.presentation

import android.util.Log
import com.loopj.android.http.AsyncHttpClient
import kz.q19.domain.model.language.Language
import kz.q19.socket.SocketClient
import kz.q19.socket.SocketClientConfig
import kz.q19.socket.listener.SocketStateListener
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.data.remote.http.AsyncHttpClientBuilder
import q19.kenes.widget.data.remote.http.ConfigsResponseHandler
import q19.kenes.widget.data.remote.http.IceServersResponseHandler
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

class KenesWidgetPresenter constructor(
    private val database: Database
) : BasePresenter<KenesWidgetView>(), SocketStateListener {

    companion object {
        private val TAG = KenesWidgetPresenter::class.java.simpleName
    }

    private var language: Language = Language.DEFAULT

    private var asyncHttpClient: AsyncHttpClient? = null

    private var socketClient: SocketClient? = null

    override fun onFirstViewAttach() {
        asyncHttpClient = AsyncHttpClientBuilder.build()

        loadConfigs()
        loadIceServers()

        initSocket()
    }

    private fun loadConfigs() {
        asyncHttpClient?.get(UrlUtil.buildUrl("/configs"), ConfigsResponseHandler(
            onSuccess = { configs ->
//                Log.d(TAG, "configs: $configs")

                database.setConfigs(configs)

                view?.showBotInfo(configs.bot)
            },
            onFailure = { throwable ->
                Log.d(TAG, "throwable: $throwable")
            }
        ))
    }

    private fun loadIceServers() {
        asyncHttpClient?.get(UrlUtil.buildUrl("/ice_servers"), IceServersResponseHandler(
            onSuccess = { iceServers ->
                database.setIceServers(iceServers)
            },
            onFailure = { throwable ->
                Log.d(TAG, "throwable: $throwable")
            }
        ))
    }

    private fun initSocket() {
        Log.d(TAG, "initSocket()")

        socketClient?.setSocketStateListener(this)

        val socketUrl = UrlUtil.getSocketUrl(UrlUtil.getHostname())
        if (socketUrl.isNullOrBlank()) {
            throw NullPointerException("Socket url is null. Please, provide a valid url.")
        }

        SocketClientConfig.init(true, language = language)
        socketClient = SocketClient.getInstance()
        socketClient?.create(socketUrl)

        socketClient?.registerSocketConnectEventListener()
        socketClient?.registerMessageEventListener()
        socketClient?.registerChatBotDashboardEventListener()
        socketClient?.registerUsersQueueEventListener()
        socketClient?.registerCallAgentGreetEventListener()
        socketClient?.registerCallAgentTypingEventListener()
        socketClient?.registerUserDialogFeedbackEventListener()
        socketClient?.registerFormInitializeEventListener()
        socketClient?.registerFormFinalizeEventListener()
        socketClient?.registerSocketDisconnectEventListener()

        socketClient?.connect()

        socketClient?.sendUserLanguage(language)
    }

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        socketClient?.setSocketStateListener(null)
        socketClient?.release()
        socketClient = null

        asyncHttpClient?.cancelAllRequests(true)
        asyncHttpClient = null

        super.onDestroy()
    }


    fun setLanguage(language: Language) {
        this.language = language
    }


    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        Log.d(TAG, "onSocketConnect()")
    }

    override fun onSocketDisconnect() {
        Log.d(TAG, "onSocketDisconnect()")
    }

}