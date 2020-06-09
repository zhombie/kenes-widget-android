package q19.kenes_widget.model

internal sealed class ViewState {
    object ChatBot : ViewState()

    class TextDialog(var state: TextDialogState) : ViewState()
    class AudioDialog(var state: MediaDialogState) : ViewState()
    class VideoDialog(var state: MediaDialogState) : ViewState()

    object DialogQualityFeedback : ViewState()

    object Form : ViewState()

    object Info : ViewState()

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

internal enum class TextDialogState {
    IDLE,
    PENDING,
    LIVE
}

internal enum class MediaDialogState {
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
