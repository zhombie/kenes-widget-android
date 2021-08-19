package q19.kenes.widget.ui.presentation.call

import kz.q19.domain.model.message.Message

internal class CallInteractor {

    companion object {
        const val MAX_UNREAD_MESSAGES_COUNT = 9
    }

    val chatMessages = mutableListOf<Message>()

    var callState: CallState = CallState.IDLE

    var unreadMessagesCount: Int = 0
        set(value) {
            if (value <= MAX_UNREAD_MESSAGES_COUNT) {
                field = value
            }
        }

    sealed class CallState {
        object IDLE : CallState()
        object Pending : CallState()
        object Live : CallState()
    }

}