package q19.kenes_widget.model

sealed class ViewState {
    object IDLE : ViewState()
    object ChatBot : ViewState()
    class VideoDialog(var dialog: Dialog?) : ViewState()
    class AudioDialog(var dialog: Dialog?) : ViewState()
}

fun ViewState.getVideoDialog(): Dialog? {
    return if (this is ViewState.VideoDialog) {
        dialog
    } else {
        null
    }
}

fun ViewState.setVideoDialog(dialog: Dialog?) {
    if (this is ViewState.VideoDialog) {
        this.dialog = dialog
    }
}