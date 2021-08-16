package q19.kenes.widget.ui.presentation.call.text

import kz.q19.domain.model.language.Language
import kz.q19.domain.model.message.Message
import kz.q19.socket.repository.SocketRepository
import q19.kenes.widget.ui.presentation.home.ChatbotInteractor
import q19.kenes.widget.ui.presentation.platform.BasePresenter

internal class TextChatPresenter constructor(
    private val language: Language,
    private val socketRepository: SocketRepository
) : BasePresenter<TextChatView>() {

    private val interactor = ChatbotInteractor()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
    }

    fun onNewMessage(message: Message) {
        addNewMessage(message)
    }

    fun onSendTextMessage(message: String?) {
        if (message.isNullOrBlank()) {
            //
        } else {
            val outgoingMessage = message.trim()

            getView().clearMessageInput()
            socketRepository.sendUserMessage(outgoingMessage)

            addNewMessage(
                Message.Builder()
                    .setType(Message.Type.OUTGOING)
                    .setText(outgoingMessage)
                    .build()
            )
        }
    }

    private fun addNewMessage(message: Message) {
        interactor.chatMessages.add(message)
        getView().showNewMessage(message)
    }

}