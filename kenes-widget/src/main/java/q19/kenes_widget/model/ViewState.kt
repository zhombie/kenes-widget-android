package q19.kenes_widget.model

internal sealed class ViewState {
    object ChatBot : ViewState()
    object CallFeedback : ViewState()
    class VideoDialog(var state: State) : ViewState()
    class AudioDialog(var state: State) : ViewState()
    object Info : ViewState()

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

internal enum class State {
    IDLE,
    PENDING,
    PREPARATION,
    LIVE,
    OPPONENT_DISCONNECT,
    USER_DISCONNECT,
    FINISHED,
    HIDDEN,
    SHOWN;

    override fun toString(): String {
        return this.name
    }
}


internal val ViewState.isOnLiveCall: Boolean
    get() = when (this) {
        is ViewState.VideoDialog -> state == State.LIVE
        is ViewState.AudioDialog -> state == State.LIVE
        else -> false
    }