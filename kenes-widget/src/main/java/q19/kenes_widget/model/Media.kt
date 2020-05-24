package q19.kenes_widget.model

internal data class Media(
    var imageUrl: String? = null,
    var fileUrl: String? = null,
    var name: String? = null,
    var ext: String? = null
) {

    companion object {
        private val IMAGE_EXTENSIONS = arrayOf(
            "png",
            "jpg",
            "jpeg"
        )
    }

    val isImage: Boolean
        get() = !imageUrl.isNullOrBlank() && IMAGE_EXTENSIONS.any { ext == it }

    val isFile: Boolean
        get() = !fileUrl.isNullOrBlank()

}