package kz.q19.kenes.widget.ui.presentation

import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.core.URLManager
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.data.local.Database
import kz.q19.kenes.widget.data.remote.http.AsyncHTTPClient
import kz.q19.kenes.widget.data.remote.http.ConfigsResponseHandler
import kz.q19.kenes.widget.data.remote.http.IceServersResponseHandler
import kz.q19.kenes.widget.ui.platform.BasePresenter
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState
import kz.q19.kenes.widget.ui.presentation.common.Screen
import kz.q19.socket.listener.SocketStateListener
import kz.q19.socket.repository.SocketRepository

internal class KenesWidgetPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<KenesWidgetView>(), SocketStateListener {

    companion object {
        private val TAG = KenesWidgetPresenter::class.java.simpleName
    }

    private var asyncHTTPClient: AsyncHTTPClient? = null

    private var bottomSheetState: BottomSheetState? = null

    override fun onFirstViewAttach() {
        asyncHTTPClient = AsyncHTTPClient.Builder()
            .build()

        loadConfigs()
        loadIceServers()

        initSocket()
    }

    private fun loadConfigs() {
        asyncHTTPClient?.get(
            URLManager.buildUrl("/configs"),
            ConfigsResponseHandler(
                onSuccess = { configs ->
                    database.setConfigs(configs)

                    getView().showBotInfo(configs.bot)
                },
                onFailure = { throwable ->
                    Logger.error(TAG, "throwable: $throwable")
                }
            )
        )
    }

    private fun loadIceServers() {
        asyncHTTPClient?.get(
            URLManager.buildUrl("/ice_servers"),
            IceServersResponseHandler(
                onSuccess = { iceServers ->
                    database.setIceServers(iceServers)
                },
                onFailure = { throwable ->
                    Logger.error(TAG, "throwable: $throwable")
                }
            )
        )
    }

    private fun initSocket() {
        Logger.debug(TAG, "initSocket()")

        socketRepository.setSocketStateListener(this)

        socketRepository.registerSocketConnectEventListener()
        socketRepository.registerMessageEventListener()
        socketRepository.registerSocketDisconnectEventListener()

        if (!socketRepository.isConnected()) {
            socketRepository.create(URLManager.getSocketUrl())

            socketRepository.connect()
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

        asyncHTTPClient?.dispose()
        asyncHTTPClient = null
    }

}