package q19.kenes_widget.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

internal object FileUtil {

    // Check If SD Card is present or not method
    val isSDCardPresent: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    fun Context.getRootDirPath(): String? {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val file = ContextCompat.getExternalFilesDirs(this, null)[0]
            file.absolutePath
        } else {
            filesDir.absolutePath
        }
    }

    fun Context.getMimeType(uri: Uri): String? {
        return if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase())
        }
    }

    fun Context.openFile(file: File) {
        val data = FileProvider.getUriForFile(this, "q19.kenes", file)
        grantUriPermission(packageName, data, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val mimeType = getMimeType(Uri.fromFile(file))
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(data, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

}