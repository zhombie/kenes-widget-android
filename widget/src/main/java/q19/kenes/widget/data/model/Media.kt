package q19.kenes.widget.data.model

import android.content.Context
import androidx.annotation.StringRes
import q19.kenes.widget.util.FileUtil.getRootDirPath
import q19.kenes_widget.R
import java.io.File

internal data class Media constructor(
    var imageUrl: String? = null,
    var audioUrl: String? = null,
    var fileUrl: String? = null,
    var hash: String? = null,
    var ext: String? = null,

    // Local variables
    var local: File? = null
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

    val isAudio: Boolean
        get() = !audioUrl.isNullOrBlank()

    val isFile: Boolean
        get() = !fileUrl.isNullOrBlank()

    fun getFile(context: Context): File {
        return local ?: File(context.getRootDirPath() + File.separatorChar + hash)
    }

    val fileTypeStringRes: Int
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
            -1
        }

}