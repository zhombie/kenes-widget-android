package q19.kenes_widget.model

internal data class Dialog(
    var callAgentId: String? = null,
    var callAgentName: String? = null,
    var callAgentAvatarUrl: String? = null,
    var media: String? = null
) {

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

    fun clear() {
        callAgentId = null
        callAgentName = null
        media = null
        isInitiator = false
        unreadMessages = 0
    }

    override fun toString(): String {
        return "Dialog(operatorId=\"$callAgentId\", media=\"$media\")"
    }

}