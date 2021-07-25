package q19.kenes.widget.ui.presentation.calls

import kz.q19.domain.model.language.Language
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.domain.model.buildCalls2
import q19.kenes.widget.ui.presentation.platform.BasePresenter

internal class CallsPresenter constructor(
    private val language: Language,
    private val database: Database,
    private val socketRepository: SocketRepository
) : BasePresenter<CallsView>() {

    companion object {
        private val TAG = CallsPresenter::class.java.simpleName
    }

    private var calls: List<Call> = emptyList()

    private val breadcrumb = mutableListOf<Call>()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        database.setOnUpdateConfigsListener { configs ->
            calls = configs.buildCalls2(language) ?: emptyList()
            getView().showMediaCalls(calls)
        }
    }

    fun onCallClicked(call: Call) {
//        getView().launchVideoCall(call)

        breadcrumb.add(call)

        if (call is CallGroup) {
            getView().showMediaCalls(call.children)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        database.setOnUpdateConfigsListener(null)
    }

    fun onGoBackButtonClicked(): Boolean {
        Logger.debug(TAG, "onGoBackButtonClicked()")

        return if (breadcrumb.isEmpty()) {
            true
        } else {
            breadcrumb.removeLast()

            if (breadcrumb.isEmpty()) {
                getView().showMediaCalls(calls)
            } else {
                getView().showMediaCalls(listOf(breadcrumb.last()))
            }

            false
        }
    }

}