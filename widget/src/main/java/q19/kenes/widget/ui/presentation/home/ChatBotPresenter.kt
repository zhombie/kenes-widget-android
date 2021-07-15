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
import q19.kenes.widget.data.remote.http.ResponseGroupsResponseHandler
import q19.kenes.widget.data.remote.http.ResponseInfoResponseHandler
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

                getView().showResponseGroups(responseGroups)
            },
            onFailure = {
            }
        ))
    }

    fun onResponseGroupClicked(responseGroup: ResponseGroup) {
        chatBot.activeResponseGroup = responseGroup

        val params = RequestParams(
            "parent_id", responseGroup.id,
            "nested", false
        )

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response_groups"), params, ResponseGroupsResponseHandler(
            onSuccess = { responseGroups ->
                Logger.debug(TAG, "onResponseGroupClicked() -> " +
                    "responseGroup: $responseGroup, " +
                    "responseGroups: $responseGroups")
                getView().showResponseGroups(responseGroups)
            },
            onFailure = {
            }
        ))
    }

    fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
        chatBot.activeResponseGroupChild = child

        val params = RequestParams(
            "response_id", child.id
        )

        asyncHttpClient?.get(UrlUtil.buildUrl("/api/kbase/response"), params, ResponseInfoResponseHandler(
            onSuccess = { response ->
                Logger.debug(TAG, "onResponseClicked() -> " +
                    "child: $child, " +
                    "response: $response")
                getView().showResponse(response)
            },
            onFailure = {
            }
        ))
    }

    fun onGoBackButtonClicked(responseGroup: ResponseGroup) {

    }

    fun onResetDataRequested() {
        if (System.currentTimeMillis() - chatBot.lastResponseGroupsLoadedTime > 60 * 1000L) {
            loadResponseGroups()
        }

        chatBot.clear()
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

        chatBot.clear()
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