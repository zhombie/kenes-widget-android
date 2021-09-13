package kz.q19.kenes.widget.ui.presentation.call

import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState

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

            if (field is CallState.IDLE || field is CallState.Pending) {
                callAgent = null
            }

            listener?.onNewCallState(field)
        }

    // Video
    var isLocalAudioEnabled: Boolean = false
    var isLocalVideoEnabled: Boolean = false
    var isRemoteAudioEnabled: Boolean = false
    var isRemoteVideoEnabled: Boolean = false

    var callAgent: CallAgent? = null

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

    data class CallAgent constructor(
        val fullName: String,
        val photoUrl: String?
    )

    interface CallStateListener {
        fun onNewBottomSheetState(state: BottomSheetState)
        fun onNewCallState(state: CallState)
    }

}