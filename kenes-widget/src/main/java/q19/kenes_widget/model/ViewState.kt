package q19.kenes_widget.model

sealed class ViewState {
    object ChatBot : ViewState()
    object CallFeedback : ViewState()
    class VideoDialog(var state: State) : ViewState()
    class AudioDialog(var state: State) : ViewState()
    object Info : ViewState()

    override fun toString(): String {
        return this.javaClass.simpleName
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
    SHOWN;

    override fun toString(): String {
        return this.name
    }
}


val ViewState.isOnLiveCall: Boolean
    get() = when (this) {
        is ViewState.VideoDialog -> state == State.LIVE
        is ViewState.AudioDialog -> state == State.LIVE
        else -> false
    }