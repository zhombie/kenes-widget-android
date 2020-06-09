package q19.kenes_widget.model

internal class Footer(
    val type: Type
) {

    enum class Type {
        GO_TO_HOME,
        SWITCH_TO_CALL_AGENT,
        FUZZY_QUESTION
    }

}
