package q19.kenes.widget.ui.presentation.home

import android.util.Log
import kz.q19.domain.model.message.Category
import kz.q19.domain.model.message.Message
import kz.q19.socket.SocketClient
import kz.q19.socket.listener.ChatBotListener
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BasePresenter

class ChatBotPresenter constructor(
    private val database: Database
) : BasePresenter<ChatBotView>(), ChatBotListener {

    companion object {
        private val TAG = ChatBotPresenter::class.java.simpleName
    }

    private var socketClient: SocketClient? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        socketClient = SocketClient.getInstance()
        socketClient?.setChatBotListener(this)

        socketClient?.requestParentCategories()
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