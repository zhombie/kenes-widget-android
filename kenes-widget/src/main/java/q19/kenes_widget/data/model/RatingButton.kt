package q19.kenes_widget.data.model

internal data class RatingButton constructor(
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