package q19.kenes.widget.ui.presentation.home

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.Message
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.model.Category
import kz.q19.socket.repository.SocketRepository
import kz.q19.utils.html.HTMLCompat
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.data.remote.http.AsyncHttpClientBuilder
import q19.kenes.widget.data.remote.http.ResponseGroupChildrenResponseHandler
import q19.kenes.widget.data.remote.http.ResponseGroupsResponseHandler
import q19.kenes.widget.data.remote.http.ResponseInfoResponseHandler
import q19.kenes.widget.domain.model.Element
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.presentation.model.ChatBot
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.UrlUtil

internal class ChatbotPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<ChatbotView>(), ChatBotListener {

    companion object {
        private val TAG = ChatbotPresenter::class.java.simpleName
    }

    private var asyncHttpClient: AsyncHttpClient? = null

    private var chatBot = ChatBot()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        asyncHttpClient = AsyncHttpClientBuilder().build()

        socketRepository.setChatBotListener(this)

        socketRepository.registerMessageEventListener()

        loadResponseGroups(true)
    }

    private fun loadResponseGroups(reload: Boolean) {
        if (!reload) {
            if (chatBot.primaryResponseGroups != null) {
                getView().showResponses(chatBot.primaryResponseGroups!!)
                return
            }
        }

        getView().showLoadingIndicator()

        val params = RequestParams(
            "nested", true
        )

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response_groups"), params, ResponseGroupsResponseHandler(
            onSuccess = { responseGroups ->
                Logger.debug(TAG, "loadResponseGroups() -> responseGroups: $responseGroups")

                getView().hideLoadingIndicator()

                chatBot.primaryResponseGroups = responseGroups
                chatBot.lastResponseGroupsLoadedTime = System.currentTimeMillis()

                getView().showResponses(responseGroups)
            },
            onFailure = {
                getView().hideLoadingIndicator()
            }
        ))
    }

    fun onResponseGroupClicked(responseGroup: ResponseGroup) {
        Logger.debug(TAG, "onResponseGroupClicked() -> $responseGroup")

        getView().showLoadingIndicator()

        chatBot.breadcrumb.add(responseGroup)

        val params = RequestParams(
            "parent_id", responseGroup.id,
            "nested", false
        )

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response_groups"), params, ResponseGroupChildrenResponseHandler(
            onSuccess = { children ->
                Logger.debug(TAG, "onResponseGroupClicked() -> " +
                    "responseGroup: $responseGroup, " +
                    "children: $children")

                getView().hideLoadingIndicator()

                val index = chatBot.breadcrumb.indexOf(responseGroup)
                chatBot.breadcrumb[index] = responseGroup.copy(children = children)

                Logger.debug(TAG, "onResponseGroupClicked() -> ${chatBot.breadcrumb}")

                getView().showResponses(listOf(chatBot.breadcrumb[index]))
            },
            onFailure = {
                getView().hideLoadingIndicator()
            }
        ))
    }

    fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
        Logger.debug(TAG, "onResponseGroupChildClicked() -> $child")

        if (child.responses.isEmpty()) {
            chatBot.breadcrumb.remove(child)
            getView().showNoResponsesFoundMessage()
            return
        }

        getView().showLoadingIndicator()

        chatBot.breadcrumb.add(child)

        Logger.debug(TAG, "onResponseGroupChildClicked() -> ${chatBot.breadcrumb}")

        val params = RequestParams(
            "response_id", child.responses.first().id
        )

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response"), params, ResponseInfoResponseHandler(
            onSuccess = { responseInfo ->
                Logger.debug(TAG, "onResponseGroupChildClicked() -> " +
                    "child: $child, " +
                    "responseInfo: $responseInfo")

                getView().hideLoadingIndicator()

                val index = chatBot.breadcrumb.indexOf(child)
                chatBot.breadcrumb[index] = child.copy(responses = listOf(responseInfo))

                Logger.debug(TAG, "onResponseGroupChildClicked() -> ${chatBot.breadcrumb}")

                getView().showResponses(listOf(chatBot.breadcrumb[index]))
            },
            onFailure = {
                getView().hideLoadingIndicator()
            }
        ))
    }

    fun onGoBackButtonClicked(element: Element) {
        onGoBackButtonClicked()
    }

    fun onGoBackButtonClicked(): Boolean {
        Logger.debug(TAG, "onGoBackButtonClicked()")

        if (chatBot.isBottomSheetExpanded) {
            getView().toggleBottomSheet()
            return false
        }

        return if (chatBot.breadcrumb.isEmpty()) {
            true
        } else {
            chatBot.breadcrumb.removeLast()

            if (chatBot.breadcrumb.isEmpty()) {
                loadResponseGroups(false)
            } else {
                getView().showResponses(listOfNotNull(chatBot.breadcrumb.last()))
            }

            false
        }
    }

    fun onBottomSheetStateChanged(isExpanded: Boolean) {
        chatBot.isBottomSheetExpanded = isExpanded
    }

    fun onCopyText() {
        if (chatBot.breadcrumb.isEmpty()) return

        val nestable = chatBot.breadcrumb.last()
        if (nestable is ResponseGroup.Child) {
            if (nestable.responses.isEmpty()) return
            val responseInfo = nestable.responses.first()
            val text = responseInfo.text
            if (!text.isNullOrBlank()) {
                getView().copyHTMLText(nestable.title, HTMLCompat.fromHtml(text), text)
            }
        }
    }

    fun onShare() {
        if (chatBot.breadcrumb.isEmpty()) return

        val nestable = chatBot.breadcrumb.last()
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
        if (System.currentTimeMillis() - chatBot.lastResponseGroupsLoadedTime > 60 * 1000L) {
            loadResponseGroups(true)
        }

        chatBot.breadcrumb.clear()
    }

    fun onSendTextMessage(message: String?) {
        if (message.isNullOrBlank()) {
            //
        } else {
            val outgoingMessage = message.trim()

            getView().clearMessageInputViewText()
            socketRepository.sendUserMessage(outgoingMessage)

            if (chatBot.chatMessages.isEmpty()) {
                getView().hideChatMessagesHeaderView()
            }

            addNewMessage(
                Message.Builder()
                    .setType(Message.Type.OUTGOING)
                    .setText(outgoingMessage)
                    .build()
            )
        }
    }

    private fun addNewMessage(message: Message) {
        chatBot.chatMessages.add(message)
        getView().addNewMessage(message)
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

        getView().addNewMessage(
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

        getView().addNewMessage(message)
    }

    override fun onCategories(categories: List<Category>) {
        Logger.debug(TAG, "onCategories() -> $categories")
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

        chatBot.isBottomSheetExpanded = false
        chatBot.breadcrumb.clear()
        chatBot.lastResponseGroupsLoadedTime = -1L
        chatBot.primaryResponseGroups = null
        chatBot.chatMessages.clear()
    }

}