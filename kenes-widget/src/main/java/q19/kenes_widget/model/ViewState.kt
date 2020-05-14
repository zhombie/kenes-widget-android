package q19.kenes_widget.model

sealed class ViewState {
    object ChatBot : ViewState()
    object CallFeedback : ViewState()
    class VideoDialog(var state: State) : ViewState()
    class AudioDialog(var state: State) : ViewState()
    object Info : ViewState()

    override fun toString(): String {
        return when (this) {
            is VideoDialog -> "VideoDialog"
            is AudioDialog -> "AudioDialog"
            ChatBot -> "ChatBot"
            CallFeedback -> "CallFeedback"
            Info -> "Info"
        }
    }
}

enum class State {
    IDLE,
    PENDING,
    PREPARATION,
    LIVE,
    OPPONENT_DISCONNECT,
    USER_DISCONNECT,
    HIDDEN,
    SHOWN
}


val ViewState.isOnLiveCall: Boolean
    get() = when (this) {
        is ViewState.VideoDialog -> state == State.LIVE
        is ViewState.AudioDialog -> state == State.LIVE
        else -> false
    }