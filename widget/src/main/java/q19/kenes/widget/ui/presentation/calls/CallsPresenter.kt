package q19.kenes.widget.ui.presentation.calls

import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BasePresenter

class CallsPresenter constructor(
    private val database: Database
) : BasePresenter<CallsView>() {

    companion object {
        private val TAG = CallsPresenter::class.java.simpleName
    }

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        database.setOnUpdateConfigsListener { configs ->
            val mediaCalls = configs.calls?.filter { it.isParent() } ?: emptyList()
            view?.showMediaCalls(mediaCalls)
        }
    }

}