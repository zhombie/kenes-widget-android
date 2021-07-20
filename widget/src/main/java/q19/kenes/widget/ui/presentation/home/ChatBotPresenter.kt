package q19.kenes.widget.ui.presentation.home

import android.util.Log
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import kz.q19.domain.model.message.Message
import kz.q19.socket.SocketClient
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.model.Category
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

internal class ChatBotPresenter constructor(
    private val database: Database
) : BasePresenter<ChatBotView>(), ChatBotListener {

    companion object {
        private val TAG = ChatBotPresenter::class.java.simpleName
    }

    private var asyncHttpClient: AsyncHttpClient? = null

    private var socketClient: SocketClient? = null

    private var chatBot = ChatBot()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        asyncHttpClient = AsyncHttpClientBuilder.build()

        socketClient = SocketClient.getInstance()
        socketClient?.setChatBotListener(this)

        loadResponseGroups()
    }

    private fun loadResponseGroups() {
        val params = RequestParams(
            "nested", true
        )

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response_groups"), params, ResponseGroupsResponseHandler(
            onSuccess = { responseGroups ->
                Logger.debug(TAG, "loadResponseGroups() -> responseGroups: $responseGroups")

                chatBot.lastResponseGroupsLoadedTime = System.currentTimeMillis()

                getView().showResponses(responseGroups)
            },
            onFailure = {
            }
        ))
    }

    fun onResponseGroupClicked(responseGroup: ResponseGroup) {
        Logger.debug(TAG, "onResponseGroupClicked() -> $responseGroup")

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

                val index = chatBot.breadcrumb.indexOf(responseGroup)
                chatBot.breadcrumb[index] = responseGroup.copy(children = children)

                Logger.debug(TAG, "onResponseGroupClicked() -> ${chatBot.breadcrumb}")

                getView().showResponses(listOf(chatBot.breadcrumb[index]))
            },
            onFailure = {
            }
        ))
    }

    fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
        Logger.debug(TAG, "onResponseGroupChildClicked() -> $child")

        if (child.responses.isEmpty()) {
            getView().showNoResponsesFoundMessage()
            return
        }

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

                val index = chatBot.breadcrumb.indexOf(child)
                chatBot.breadcrumb[index] = child.copy(responses = listOf(responseInfo))

                Logger.debug(TAG, "onResponseGroupChildClicked() -> ${chatBot.breadcrumb}")

                getView().showResponses(listOf(chatBot.breadcrumb[index]))
            },
            onFailure = {
            }
        ))
    }

    fun onGoBackButtonClicked(element: Element) {
        onGoBackButtonClicked()
    }

    fun onGoBackButtonClicked(): Boolean {
        Logger.debug(TAG, "onGoBackButtonClicked()")

        if (chatBot.breadcrumb.isEmpty()) return true

        return when {
            chatBot.breadcrumb.isNotEmpty() -> {
                Logger.debug(TAG, "onGoBackButtonClicked() -> ${chatBot.breadcrumb}")

                chatBot.breadcrumb.removeLast()
                Logger.debug(TAG, "onGoBackButtonClicked() -> ${chatBot.breadcrumb}")
                if (chatBot.breadcrumb.isEmpty()) {
                    loadResponseGroups()
                } else {
                    getView().showResponses(listOfNotNull(chatBot.breadcrumb.last()))
                }

                false
            }
            else -> {
                loadResponseGroups()
                true
            }
        }
    }

    fun onResetDataRequested() {
        if (System.currentTimeMillis() - chatBot.lastResponseGroupsLoadedTime > 60 * 1000L) {
            loadResponseGroups()
        }

        chatBot.breadcrumb.clear()
    }


    /**
     * [BasePresenter] implementation
     */

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        socketClient?.setChatBotListener(null)
        socketClient = null

        asyncHttpClient?.cancelAllRequests(true)
        asyncHttpClient = null

        chatBot.breadcrumb.clear()
    }


    /**
     * [ChatBotListener] implementation
     */

    override fun onFuzzyTaskOffered(text: String, timestamp: Long): Boolean {
        Log.d(TAG, "onFuzzyTaskOffered() -> $text, $timestamp")
        return true
    }

    override fun onNoResultsFound(text: String, timestamp: Long): Boolean {
        Log.d(TAG, "onNoResultsFound() -> $text, $timestamp")
        return true
    }

    override fun onMessage(message: Message) {
        Log.d(TAG, "onMessage() -> $message")
    }

    override fun onCategories(categories: List<Category>) {
        Log.d(TAG, "onCategories() -> $categories")
    }

}