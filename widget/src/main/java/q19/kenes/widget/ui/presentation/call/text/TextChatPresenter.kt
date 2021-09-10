package q19.kenes.widget.ui.presentation.call.text

import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.Message
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.ui.presentation.call.CallInteractor
import q19.kenes.widget.ui.presentation.platform.BasePresenter

internal class TextChatPresenter constructor(
    private val language: Language,
    private val socketRepository: SocketRepository
) : BasePresenter<TextChatView>() {

    private val interactor = CallInteractor()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
    }

    fun onNewChatMessage(message: Message) {
        addNewChatMessage(message)
    }

    private fun addNewChatMessage(message: Message) {
        interactor.chatMessages.add(message)
        getView().showNewMessage(message)
    }

    override fun onDestroy() {
        interactor.chatMessages.clear()
    }

}