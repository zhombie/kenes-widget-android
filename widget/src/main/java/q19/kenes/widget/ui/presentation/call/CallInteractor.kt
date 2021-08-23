package q19.kenes.widget.ui.presentation.call

import kz.q19.domain.model.message.Message

internal class CallInteractor {

    companion object {
        const val MAX_UNREAD_MESSAGES_COUNT = 9
    }

    // UI
    var bottomSheetState: BottomSheetState = BottomSheetState.COLLAPSED
        set(value) {
            field = value
            listener?.onNewBottomSheetState(field)
        }

    // Text chat
    val chatMessages = mutableListOf<Message>()

    var unreadMessagesCount: Int = 0
        set(value) {
            if (value <= MAX_UNREAD_MESSAGES_COUNT) {
                field = value
            }
        }

    // Call
    var callState: CallState = CallState.IDLE
        set(value) {
            field = value
            listener?.onNewCallState(field)
        }

    var listener: CallStateListener? = null

    sealed class CallState {
        object IDLE : CallState()
        object Pending : CallState()
        object Live : CallState()
        sealed class Disconnected : CallState() {
            object User : Disconnected()
            object CallAgent : Disconnected()
            object Timeout : Disconnected()
        }
    }

    interface CallStateListener {
        fun onNewBottomSheetState(state: BottomSheetState)
        fun onNewCallState(state: CallState)
    }

}