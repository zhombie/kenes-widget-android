package q19.kenes_widget.model

internal class Dialog {

    companion object {
        const val MAX_UNREAD_MESSAGES_COUNT = 9
    }

    var isInitiator = false

    var unreadMessages: Int = 0
        set(value) {
            if (field >= MAX_UNREAD_MESSAGES_COUNT) {
                return
            } else {
                field = value
            }
        }

    var isActive = false

    fun clear() {
        isInitiator = false
        unreadMessages = 0
        isActive = false
    }

}