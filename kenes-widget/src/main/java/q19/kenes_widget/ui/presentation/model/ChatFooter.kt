package q19.kenes_widget.ui.presentation.model

internal class ChatFooter constructor(
    val type: Type
) {

    enum class Type {
        GO_TO_HOME,
        SWITCH_TO_CALL_AGENT,
        FUZZY_QUESTION
    }

}
