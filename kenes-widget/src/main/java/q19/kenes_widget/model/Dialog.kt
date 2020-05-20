package q19.kenes_widget.model

data class Dialog(
    var operatorId: String? = null,
    var instance: String? = null,
    var media: String? = null
) {

    var isInitiator = false

    val isOnLive: Boolean
        get() = !operatorId.isNullOrBlank() && !instance.isNullOrBlank() && !media.isNullOrBlank()

    fun clear() {
        operatorId = null
        instance = null
        media = null
        isInitiator = false
    }

    override fun toString(): String {
        return "Dialog(operatorId=\"$operatorId\", instance=\"$instance\", media=\"$media\")"
    }

}