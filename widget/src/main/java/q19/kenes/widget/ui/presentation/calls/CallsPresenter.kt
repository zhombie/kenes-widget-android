package q19.kenes.widget.ui.presentation.calls

import kz.q19.domain.model.language.Language
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BasePresenter

class CallsPresenter constructor(
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<CallsView>() {

    companion object {
        private val TAG = CallsPresenter::class.java.simpleName
    }

    private var language: Language? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        database.setOnUpdateConfigsListener { configs ->
            val mediaCalls = configs.calls?.filter { it.isParent() } ?: emptyList()
            getView().showMediaCalls(mediaCalls.mapNotNull {
                val title = it.title.get(language) ?: it.title.ru
                if (title.isNullOrBlank()) return@mapNotNull null
                Call(title)
            })
        }
    }

    fun onCallClicked(call: Call) {
        getView().launchVideoCall(call)
    }

}