package q19.kenes_widget.model

import androidx.annotation.StringRes
import q19.kenes_widget.R

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

    val fileTypeStringRes: Int?
        @StringRes
        get() = if (isFile) {
            when (ext) {
                "txt" -> R.string.kenes_text_file
                "doc", "docx" -> R.string.kenes_microsoft_word_document
                "xls", "xlsx" -> R.string.kenes_microsoft_excel_document
                "pdf" -> R.string.kenes_pdf_file
                "html" -> R.string.kenes_html_text
                else -> R.string.kenes_file
            }
        } else {
            null
        }

}