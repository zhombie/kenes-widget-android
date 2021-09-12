package kz.q19.kenes.widget.ui.presentation.call

import kz.q19.domain.model.call.Call
import kz.q19.domain.model.call.CallGroup
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.Message
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.model.Category
import kz.q19.socket.repository.SocketRepository
import kz.q19.kenes.widget.core.device.DeviceInfo
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.data.local.Database
import kz.q19.kenes.widget.domain.model.buildCallsAsTree
import kz.q19.kenes.widget.ui.presentation.call.selection.CallSelection
import kz.q19.kenes.widget.ui.presentation.platform.BasePresenter

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

        database.setOnUpdateConfigsListener { configs ->
            interactor.anyCalls = configs.buildCallsAsTree(language) ?: emptyList()
            getView().showCalls(interactor.anyCalls)
        }
    }

    override fun onViewResume() {
        super.onViewResume()

        initSocket()
    }

    private fun initSocket() {
        socketRepository.setChatBotListener(this)

        socketRepository.registerMessageEventListener()
    }

    fun onCallGroupClicked(callGroup: CallGroup) {
        if (callGroup.children.all { it is Call }) {
            getView().showCallSelection(
                CallSelection(
                    callGroup.id,
                    callGroup.title,
                    callGroup.children.filterIsInstance<Call>()
                )
            )
        } else {
            interactor.breadcrumb.add(callGroup)

            getView().showCalls(callGroup.children)
        }
    }

    fun onCallSelected(call: Call) {
        getView().tryToResolvePermissions(call)
    }

    fun onCallPermissionsGranted(call: Call) {
        getView().launchCall(call)
    }

    fun onBackPressed(): Boolean {
        Logger.debug(TAG, "onBackPressed()")

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

    fun onResetDataRequested() {
        interactor.breadcrumb.clear()

        getView().showCalls(interactor.anyCalls)
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