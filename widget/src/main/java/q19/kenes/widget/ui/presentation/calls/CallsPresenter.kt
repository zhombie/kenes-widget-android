package q19.kenes.widget.ui.presentation.calls

import kz.q19.socket.repository.SocketRepository
import kz.q19.webrtc.PeerConnectionClient
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.ui.presentation.platform.BasePresenter

class CallsPresenter constructor(
    private val database: Database,
    private val peerConnectionClient: PeerConnectionClient,
    private val socketRepository: SocketRepository
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