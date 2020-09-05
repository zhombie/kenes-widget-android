package q19.kenes_widget.ui.presentation.model

internal sealed class ViewState {
    sealed class ChatBot : ViewState() {
        class Categories(val isLoading: Boolean) : ChatBot()
        class UserPrompt(val isLoading: Boolean) : ChatBot()
    }

    sealed class TextDialog : ViewState() {
        object IDLE : TextDialog()
        object Pending : TextDialog()
        object Live : TextDialog()
        object UserDisconnected : TextDialog()
        object CallAgentDisconnected : TextDialog()
        class UserFeedback(val isFeedbackSent: Boolean) : TextDialog()
    }

    object OperatorCall : ViewState()

    sealed class AudioDialog : ViewState() {
        object Pending : AudioDialog()

        object Start : AudioDialog()
        object Preparation : AudioDialog()
        object Ready : AudioDialog()
        class Live(val isDialogScreenShown: Boolean = true) : AudioDialog()
        object UserDisconnected : AudioDialog()
        object CallAgentDisconnected : AudioDialog()

        class UserFeedback(val isFeedbackSent: Boolean) : AudioDialog()
    }

    sealed class VideoDialog : ViewState() {
        object Pending : VideoDialog()

        object Start : VideoDialog()
        object Preparation : VideoDialog()
        object Ready : VideoDialog()
        class Live(val isDialogScreenShown: Boolean = true) : VideoDialog()
        object UserDisconnected : VideoDialog()
        object CallAgentDisconnected : VideoDialog()

        class UserFeedback(val isFeedbackSent: Boolean) : VideoDialog()
    }

    object Form : ViewState()

    object DynamicForm : ViewState()

//    object Contacts : ViewState()

    sealed class Services : ViewState() {
        object IDLE : Services()
        object Process : Services()
        object Cancelled : Services()
        object Pending : Services()
        object Completed : Services()
    }

    object Info : ViewState()
}