package q19.kenes_widget.model

import android.content.Context
import q19.kenes_widget.util.FileUtil.getRootDirPath
import java.io.File

internal data class Attachment(
    var title: String? = null,
    var ext: String? = null,
    var type: String? = null,
    var url: String? = null
) {

    fun getFile(context: Context): File {
        return File(context.getRootDirPath() + File.separatorChar + title)
    }

}