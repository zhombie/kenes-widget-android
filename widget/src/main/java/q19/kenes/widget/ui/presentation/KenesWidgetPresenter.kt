package q19.kenes.widget.ui.presentation

import com.loopj.android.http.AsyncHttpClient
import kz.q19.domain.model.language.Language
import kz.q19.socket.listener.SocketStateListener
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.data.remote.http.AsyncHttpClientBuilder
import q19.kenes.widget.data.remote.http.ConfigsResponseHandler
import q19.kenes.widget.data.remote.http.IceServersResponseHandler
import q19.kenes.widget.ui.presentation.common.BottomSheetState
import q19.kenes.widget.ui.presentation.common.Screen
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class KenesWidgetPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<KenesWidgetView>(), SocketStateListener {

    companion object {
        private val TAG = KenesWidgetPresenter::class.java.simpleName
    }

    private var asyncHttpClient: AsyncHttpClient? = null

    private var bottomSheetState: BottomSheetState? = null

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
                Logger.error(TAG, "throwable: $throwable")
            }
        ))
    }

    private fun loadIceServers() {
        asyncHttpClient?.get(UrlUtil.buildUrl("/ice_servers"), IceServersResponseHandler(
            onSuccess = { iceServers ->
                database.setIceServers(iceServers)
            },
            onFailure = { throwable ->
                Logger.error(TAG, "throwable: $throwable")
            }
        ))
    }

    private fun initSocket() {
        Logger.debug(TAG, "initSocket()")

        socketRepository.setSocketStateListener(this)

        socketRepository.registerSocketConnectEventListener()
        socketRepository.registerMessageEventListener()
        socketRepository.registerSocketDisconnectEventListener()

        if (!socketRepository.isConnected()) {
            val url = UrlUtil.getSocketUrl()
            if (!url.isNullOrBlank()) {
                socketRepository.create(url)

                socketRepository.connect()
            }
        }
    }

    fun onBottomNavigationButtonSelected(screen: Screen) {
        if (bottomSheetState == BottomSheetState.EXPANDED) {
            if (screen == Screen.HOME) {
                getView().showBottomSheetCloseButton()
            } else {
                getView().hideBottomSheetCloseButton()
            }
        }

        getView().navigateTo(screen.index)
    }

    fun onBottomSheetStateChanged(state: BottomSheetState) {
        bottomSheetState = state

        if (state == BottomSheetState.COLLAPSED) {
            getView().hideBottomSheetCloseButton()
        } else {
            getView().showBottomSheetCloseButton()
        }
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        Logger.debug(TAG, "onSocketConnect()")

        socketRepository.sendUserLanguage(language)
    }

    override fun onSocketDisconnect() {
        Logger.debug(TAG, "onSocketDisconnect()")
    }


    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        Logger.debug(TAG, "onDestroy()")

        bottomSheetState = null

        socketRepository.removeAllListeners()
        socketRepository.release()

        asyncHttpClient?.cancelAllRequests(true)
        asyncHttpClient = null
    }

}