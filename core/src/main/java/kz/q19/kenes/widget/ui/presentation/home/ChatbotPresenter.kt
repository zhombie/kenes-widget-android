package kz.q19.kenes.widget.ui.presentation.home

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.core.UrlManager
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.data.local.Database
import kz.q19.kenes.widget.data.remote.http.AsyncHttpClientBuilder
import kz.q19.kenes.widget.data.remote.http.ResponseGroupChildrenResponseHandler
import kz.q19.kenes.widget.data.remote.http.ResponseGroupsResponseHandler
import kz.q19.kenes.widget.data.remote.http.ResponseInfoResponseHandler
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState
import kz.q19.kenes.widget.ui.presentation.platform.BasePresenter
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.model.Category
import kz.q19.socket.repository.SocketRepository
import kz.q19.utils.html.HTMLCompat

internal class ChatbotPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<ChatbotView>(), ChatBotListener {

    companion object {
        private val TAG = ChatbotPresenter::class.java.simpleName
    }

    private var asyncHttpClient: AsyncHttpClient? = null

    private val interactor = ChatbotInteractor()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        asyncHttpClient = AsyncHttpClientBuilder().build()

        loadResponseGroups(true)
    }

    override fun onViewResume() {
        super.onViewResume()

        initSocket()
    }

    private fun initSocket() {
        socketRepository.setChatBotListener(this)

        socketRepository.registerMessageEventListener()
    }

    private fun loadResponseGroups(reload: Boolean) {
        if (!reload) {
            if (interactor.primaryResponseGroups.isNotEmpty()) {
                getView().showResponses(interactor.primaryResponseGroups)
                return
            }
        }

        getView().showLoadingIndicator()

        val params = RequestParams(
            "nested", true
        )

        asyncHttpClient?.get(
            UrlManager.buildUrl("/api/kbase/response_groups"),
            params,
            ResponseGroupsResponseHandler(
                onSuccess = { responseGroups ->
//                    Logger.debug(TAG, "loadResponseGroups() -> responseGroups: $responseGroups")

                    getView().hideLoadingIndicator()

                    interactor.primaryResponseGroups = responseGroups
                    interactor.lastResponseGroupsLoadedTime = System.currentTimeMillis()

                    getView().showResponses(responseGroups)
                },
                onFailure = {
                    Logger.debug(TAG, "onFailure(): $it")

                    getView().hideLoadingIndicator()
                }
            )
        )
    }

    fun onResponseGroupClicked(responseGroup: ResponseGroup) {
        Logger.debug(TAG, "onResponseGroupClicked() -> $responseGroup")

        getView().showLoadingIndicator()

        interactor.breadcrumb.add(responseGroup)

        val params = RequestParams(
            "parent_id", responseGroup.id,
            "nested", false
        )

        asyncHttpClient?.get(
            UrlManager.buildUrl("/api/kbase/response_groups"),
            params,
            ResponseGroupChildrenResponseHandler(
                onSuccess = { children ->
//                    Logger.debug(TAG, "onResponseGroupClicked() -> " +
//                        "responseGroup: $responseGroup, " +
//                        "children: $children")

                    getView().hideLoadingIndicator()

                    val index = interactor.breadcrumb.indexOf(responseGroup)
                    interactor.breadcrumb[index] = responseGroup.copy(children = children)

                    Logger.debug(TAG, "onResponseGroupClicked() -> ${interactor.breadcrumb}")

                    getView().showResponses(listOf(interactor.breadcrumb[index]))
                },
                onFailure = {
                    Logger.debug(TAG, "onFailure(): $it")

                    getView().hideLoadingIndicator()

                    val index = interactor.breadcrumb.indexOf(responseGroup)
                    getView().showResponses(listOf(interactor.breadcrumb[index]))
                }
            )
        )
    }

    fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
        Logger.debug(TAG, "onResponseGroupChildClicked() -> $child")

        if (child.responses.isEmpty()) {
            interactor.breadcrumb.remove(child)
            getView().showNoResponsesFoundMessage()
            return
        }

        getView().showLoadingIndicator()

        interactor.breadcrumb.add(child)

        Logger.debug(TAG, "onResponseGroupChildClicked() -> ${interactor.breadcrumb}")

        val params = RequestParams(
            "response_id", child.responses.first().id
        )

        asyncHttpClient?.get(
            UrlManager.buildUrl("/api/kbase/response"),
            params,
            ResponseInfoResponseHandler(
                onSuccess = { responseInfo ->
//                    Logger.debug(TAG, "onResponseGroupChildClicked() -> " +
//                        "child: $child, " +
//                        "responseInfo: $responseInfo")

                    getView().hideLoadingIndicator()

                    val index = interactor.breadcrumb.indexOf(child)
                    interactor.breadcrumb[index] = child.copy(responses = listOf(responseInfo))

                    Logger.debug(TAG, "onResponseGroupChildClicked() -> ${interactor.breadcrumb}")

                    getView().showResponses(listOf(interactor.breadcrumb[index]))
                },
                onFailure = {
                    Logger.debug(TAG, "onFailure(): $it")

                    getView().hideLoadingIndicator()

                    val index = interactor.breadcrumb.indexOf(child)
                    getView().showResponses(listOf(interactor.breadcrumb[index]))
                }
            )
        )
    }

    fun onBackPressed(): Boolean {
        Logger.debug(TAG, "onGoBackButtonClicked()")

        if (interactor.bottomSheetState == BottomSheetState.EXPANDED) {
            getView().collapseBottomSheet()
            return false
        }

        return if (interactor.breadcrumb.isEmpty()) {
            true
        } else {
            interactor.breadcrumb.removeLast()

            if (interactor.breadcrumb.isEmpty()) {
                loadResponseGroups(false)
            } else {
                getView().showResponses(listOfNotNull(interactor.breadcrumb.last()))
            }

            false
        }
    }

    fun onBottomSheetStateChanged(state: BottomSheetState) {
        interactor.bottomSheetState = state
    }

    fun onCopyResponseText() {
        if (interactor.breadcrumb.isEmpty()) return

        val nestable = interactor.breadcrumb.last()
        if (nestable is ResponseGroup.Child) {
            if (nestable.responses.isEmpty()) return
            val responseInfo = nestable.responses.first()
            val text = responseInfo.text
            if (!text.isNullOrBlank()) {
                getView().copyHTMLText(nestable.title, HTMLCompat.fromHtml(text), text)
            }
        }
    }

    fun onShareResponse() {
        if (interactor.breadcrumb.isEmpty()) return

        val nestable = interactor.breadcrumb.last()
        if (nestable is ResponseGroup.Child) {
            if (nestable.responses.isEmpty()) return
            val responseInfo = nestable.responses.first()
            val text = responseInfo.text
            if (!text.isNullOrBlank()) {
                getView().share(nestable.title, HTMLCompat.fromHtml(text), text)
            }
        }
    }

    fun onResetDataRequested() {
        interactor.breadcrumb.clear()

        if (System.currentTimeMillis() - interactor.lastResponseGroupsLoadedTime > 60 * 1000L) {
            loadResponseGroups(true)
        } else {
            if (interactor.primaryResponseGroups.isNotEmpty()) {
                getView().showResponses(interactor.primaryResponseGroups)
            }
        }
    }

    fun onSendTextMessage(message: String?) {
        if (message.isNullOrBlank()) {
            //
        } else {
            val outgoingMessage = message.trim()

            getView().clearMessageInput()
            socketRepository.sendUserMessage(outgoingMessage)

            if (interactor.chatMessages.isEmpty()) {
                getView().hideChatMessagesHeader()
            }

            addNewChatMessage(
                Message.Builder()
                    .setType(Message.Type.OUTGOING)
                    .setText(outgoingMessage)
                    .build()
            )
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

        addNewChatMessage(
            Message.Builder()
                .setType(Message.Type.INCOMING)
                .setText(text)
                .setCreatedAt(timestamp)
                .build()
        )

        return true
    }

    override fun onMessage(message: Message) {
        Logger.debug(TAG, "onMessage() -> $message")

        addNewChatMessage(message)
    }

    override fun onCategories(categories: List<Category>) {
        Logger.debug(TAG, "onCategories() -> $categories")
    }

    private fun addNewChatMessage(message: Message) {
        interactor.chatMessages.add(message)
        getView().showNewChatMessage(message)
    }

    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        Logger.debug(TAG, "onDestroy()")

        socketRepository.unregisterMessageEventListener()
        socketRepository.setChatBotListener(null)

        asyncHttpClient?.cancelAllRequests(true)
        asyncHttpClient = null

        interactor.breadcrumb.clear()
        interactor.lastResponseGroupsLoadedTime = -1L
        interactor.primaryResponseGroups = emptyList()
        interactor.chatMessages.clear()
    }

}