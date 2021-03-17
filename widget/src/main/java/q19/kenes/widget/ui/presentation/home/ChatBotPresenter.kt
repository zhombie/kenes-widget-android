package q19.kenes.widget.ui.presentation.home

import android.util.Log
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.message.Message
import kz.q19.socket.SocketClient
import kz.q19.socket.listener.ChatBotListener
import kz.q19.socket.model.Category
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.data.remote.http.AsyncHttpClientBuilder
import q19.kenes.widget.data.remote.http.ResponseGroupsResponseHandler
import q19.kenes.widget.data.remote.http.ResponseInfoResponseHandler
import q19.kenes.widget.ui.presentation.model.ChatBot
import q19.kenes.widget.ui.presentation.platform.BasePresenter
import q19.kenes.widget.util.Logger
import q19.kenes.widget.util.UrlUtil

class ChatBotPresenter constructor(
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
        val params = RequestParams("nested", true)

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response_groups"), params, ResponseGroupsResponseHandler(
            onSuccess = { responseGroups ->
                Logger.debug(TAG, "responseGroups: ${responseGroups.sortedBy { it.extra.order }}")
                getView().showResponseGroups(responseGroups.sortedBy { it.extra.order })
            },
            onFailure = {
            }
        ))
    }

    fun onResponseGroupClicked(responseGroup: ResponseGroup) {
        chatBot.activeResponseGroup = responseGroup

        val params = RequestParams("parent_id", responseGroup.id, "nested", false)

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response_groups"), params, ResponseGroupsResponseHandler(
            onSuccess = { responseGroups ->
                Logger.debug(TAG, "onResponseGroupClicked() -> responseGroup: $responseGroup, responseGroups: $responseGroups")
                getView().showResponseGroups(responseGroups)
            },
            onFailure = {
            }
        ))
    }

    fun onResponseClicked(response: Response) {
        chatBot.activeResponse = response

        val params = RequestParams("response_id", response.id)

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response"), params, ResponseInfoResponseHandler(
            onSuccess = { responseInfo ->
                Logger.debug(TAG, "onResponseClicked() -> response: $response, responseInfo: $responseInfo")
                getView().showResponseInfo(responseInfo)
            },
            onFailure = {
            }
        ))
    }

    fun onGoBackButtonClicked(responseGroup: ResponseGroup) {

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

        super.onDestroy()
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
        if (categories.isEmpty()) return

//        if (categories.all { it.parentId == Category.NO_PARENT_ID }) {
//            chatBot.responseGroups = categories
//                .filter { !it.title.isNullOrBlank() }
//                .map { it.toResponseGroup() }

//            Logger.debug(TAG, "onCategories() -> if chatBot.responseGroups: ${chatBot.responseGroups}")
//        } else {
//            categories.forEach { category ->
//                chatBot.responseGroups.forEach { baseResponse ->
//                    if (baseResponse.id == category.parentId) {
//                        Logger.debug(TAG, "baseResponse.id == category.parentId: $baseResponse, $category")
//                        if (category.isResponseGroup()) {
//                            baseResponse.children.add(category.toResponseGroup())
//                        } else {
//                            baseResponse.children.add(category.toResponse())
//                        }
//                    }
//                }
//            }

//            Logger.debug(TAG, "onCategories() -> else chatBot.baseResponses: ${chatBot.responseGroups}")
//        }

//        if (chatBot.responseGroups.all { it.children.isNotEmpty() }) {
//            view?.showResponseGroups(chatBot.responseGroups)
//        }

//        if (!chatBot.isParentResponseGroupChildrenRequested) {
//            chatBot.isParentResponseGroupChildrenRequested = true
//
//            chatBot.responseGroups.forEach { baseResponse ->
//                socketClient?.getCategories(baseResponse.id)
//            }
//        }
    }

}