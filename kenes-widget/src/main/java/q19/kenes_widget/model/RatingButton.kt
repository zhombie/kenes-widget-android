package q19.kenes_widget.model

internal data class RatingButton(
    var title: String?,
    var payload: String? = null
) {

    private val splitPayload: List<String>?
        get() = payload?.split(":")

    val rating: Int
        get() = splitPayload?.get(1)?.toInt() ?: -1

    val chatId: Long
        get() = splitPayload?.get(2)?.toLong() ?: -1

}