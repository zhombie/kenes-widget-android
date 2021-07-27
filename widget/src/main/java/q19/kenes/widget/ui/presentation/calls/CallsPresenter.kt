package q19.kenes.widget.ui.presentation.calls

import kz.q19.domain.model.call.CallType
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.Message
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.model.CallInitialization
import kz.q19.socket.model.Category
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.core.device.DeviceInfo
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.domain.model.buildCallsAsTree
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class CallsPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val deviceInfo: DeviceInfo,
    private val socketRepository: SocketRepository
) : BasePresenter<CallsView>(), ChatBotListener {

    companion object {
        private val TAG = CallsPresenter::class.java.simpleName
    }

    private val interactor = CallsInteractor()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        initSocket()

        database.setOnUpdateConfigsListener { configs ->
            interactor.anyCalls = configs.buildCallsAsTree(language) ?: emptyList()
            getView().showCalls(interactor.anyCalls)
        }
    }

    private fun initSocket() {
        socketRepository.setChatBotListener(this)

        socketRepository.registerMessageEventListener()
    }

    fun onCallGroupClicked(callGroup: CallGroup) {
        interactor.breadcrumb.add(callGroup)

        getView().showCalls(callGroup.children)
    }

    fun onCallClicked(call: Call) {
        if (interactor.lastCall == null) {
            getView().tryToResolvePermissions(call)
        }
    }

    fun onCallPermissionsGranted(call: Call) {
        interactor.lastCall = call

        getView().launchPendingCall(call)
    }

    fun onBottomSheetStateChanged(isExpanded: Boolean) {
        interactor.isBottomSheetExpanded = isExpanded

        if (isExpanded) {
            val lastCall = interactor.lastCall
            if (lastCall != null) {
                interactor.lastCall = null
                onPendingCallLaunched(lastCall)
            }
        }
    }

    private fun onPendingCallLaunched(call: Call) {
        val callType = when (call) {
            is Call.Text -> CallType.TEXT
            is Call.Audio -> CallType.AUDIO
            is Call.Video -> CallType.VIDEO
        }

        socketRepository.sendCallInitialization(
            CallInitialization(
                callType = callType,
                domain = UrlUtil.getHostname()?.removePrefix("https://"),
                topic = call.topic,
                device = CallInitialization.Device(
                    os = deviceInfo.os,
                    osVersion = deviceInfo.osVersion,
                    appVersion = deviceInfo.versionName,
                    name = deviceInfo.deviceName,
                    mobileOperator = deviceInfo.operator,
                    battery = CallInitialization.Device.Battery(
                        percentage = deviceInfo.batteryPercent,
                        isCharging = deviceInfo.isPhoneCharging,
                        temperature = deviceInfo.batteryTemperature
                    )
                ),
                language = Language.RUSSIAN
            )
        )
    }

    fun onBackPressed(): Boolean {
        Logger.debug(TAG, "onGoBackButtonClicked()")

        if (interactor.isBottomSheetExpanded) {
            getView().toggleBottomSheet()
            return false
        }

        return if (interactor.breadcrumb.isEmpty()) {
            true
        } else {
            interactor.breadcrumb.removeLast()

            if (interactor.breadcrumb.isEmpty()) {
                getView().showCalls(interactor.anyCalls)
            } else {
                val last = interactor.breadcrumb.last()
                if (last is CallGroup) {
                    getView().showCalls(last.children)
                } else {
                    getView().showCalls(listOf(last))
                }
            }

            false
        }
    }

    /**
     * [ChatBotListener] implementation
     */

    override fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean {
        Logger.debug(TAG, "onFuzzyTaskOffered() -> $text, $timestamp")
        return true
    }

    override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
        Logger.debug(TAG, "onNoResultsFound() -> $text, $timestamp")
        return true
    }

    override fun onMessage(message: Message) {
        Logger.debug(TAG, "onMessage() -> $message")
    }

    override fun onCategories(categories: List<Category>) {
        Logger.debug(TAG, "onCategories() -> $categories")
    }

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        socketRepository.unregisterMessageEventListener()
        socketRepository.setChatBotListener(null)

        database.setOnUpdateConfigsListener(null)
    }

}