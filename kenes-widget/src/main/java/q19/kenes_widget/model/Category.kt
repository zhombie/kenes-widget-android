package q19.kenes_widget.model

internal data class Category(
    var id: Long,
    var title: String,
    var lang: Int,
    var parentId: Long? = null,
    var photo: String? = null
)