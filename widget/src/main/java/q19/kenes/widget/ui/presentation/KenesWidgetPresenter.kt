package q19.kenes.widget.ui.presentation

import android.util.Log
import com.loopj.android.http.AsyncHttpClient
import kz.q19.domain.model.language.Language
import kz.q19.socket.listener.SocketStateListener
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.data.remote.http.AsyncHttpClientBuilder
import q19.kenes.widget.data.remote.http.ConfigsResponseHandler
import q19.kenes.widget.data.remote.http.IceServersResponseHandler
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class KenesWidgetPresenter constructor(
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<KenesWidgetView>(), SocketStateListener {

    companion object {
        private val TAG = KenesWidgetPresenter::class.java.simpleName
    }

    private var language: Language = Language.DEFAULT

    private var asyncHttpClient: AsyncHttpClient? = null

    override fun onFirstViewAttach() {
        asyncHttpClient = AsyncHttpClientBuilder().build()

        loadConfigs()
        loadIceServers()

        initSocket()
    }

    private fun loadConfigs() {
        asyncHttpClient?.get(UrlUtil.buildUrl("/configs"), ConfigsResponseHandler(
            onSuccess = { configs ->
                database.setConfigs(configs)

                getView().showBotInfo(configs.bot)
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

        socketRepository.setSocketStateListener(this)

        socketRepository.registerSocketConnectEventListener()
        socketRepository.registerMessageEventListener()
        socketRepository.registerSocketDisconnectEventListener()

        val url = UrlUtil.getSocketUrl()
        if (!url.isNullOrBlank()) {
            socketRepository.create(url)
        }

        if (!socketRepository.isConnected()) {
            socketRepository.connect()
        }
    }

    fun onBottomNavigationButtonSelected(index: Int) {
        getView().navigateTo(index)
    }

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        socketRepository.removeAllListeners()
        socketRepository.release()

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
        socketRepository.sendUserLanguage(language)
    }

    override fun onSocketDisconnect() {
        Log.d(TAG, "onSocketDisconnect()")
    }

}