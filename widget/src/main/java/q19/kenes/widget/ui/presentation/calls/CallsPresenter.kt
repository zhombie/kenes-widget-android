package q19.kenes.widget.ui.presentation.calls

import kz.q19.domain.model.language.Language
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BasePresenter

internal class CallsPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<CallsView>() {

    companion object {
        private val TAG = CallsPresenter::class.java.simpleName
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        database.setOnUpdateConfigsListener { configs ->
            val primaryMediaCalls = configs.calls?.filter { it.isParent() } ?: emptyList()
            val calls = primaryMediaCalls.mapNotNull {
                val title = it.title.get(language) ?: it.title.get(Language.DEFAULT)
                if (title.isNullOrBlank()) return@mapNotNull null
                Call(title)
            }
            getView().showMediaCalls(calls)
        }
    }

    fun onCallClicked(call: Call) {
        getView().launchVideoCall(call)
    }

    override fun onDestroy() {
        super.onDestroy()

        database.setOnUpdateConfigsListener(null)
    }

}