package q19.kenes_widget.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

object FileUtil {

    private val IMAGE_ENTENSIONS = setOf("jpg", "jpeg", "png")
    private val AUDIO_ENTENSIONS = setOf("mp3", "wav", "opus", "ogg")
    private val VIDEO_ENTENSIONS = setOf("mp4", "mov", "webm", "mkv", "avi")
    private val DOCUMENT_ENTENSIONS = setOf("doc", "docx", "xls", "xlsx", "pdf")

    private val ALL_EXTENSIONS = mapOf(
        IMAGE_ENTENSIONS to "image",
        AUDIO_ENTENSIONS to "audio",
        VIDEO_ENTENSIONS to "video"
    )

    fun Context.getRootDirPath(): String {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val file = ContextCompat.getExternalFilesDirs(this, null)[0]
            file.absolutePath
        } else {
            filesDir.absolutePath
        }
    }

    fun Uri.getMimeType(context: Context): String? {
        return if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            context.contentResolver.getType(this)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.getDefault()))
        }
    }

    fun openFile(appCompatActivity: AppCompatActivity, file: File) {
        val uri = FileProvider.getUriForFile(appCompatActivity, "${appCompatActivity.packageName}.provider", file)
        appCompatActivity.grantUriPermission(appCompatActivity.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val mimeType = Uri.fromFile(file).getMimeType(appCompatActivity)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        appCompatActivity.startActivity(intent)
    }

    fun File.getFileType(): String? {
        if (!exists()) return null
        if (name.isNullOrBlank()) return null

        for (entry in ALL_EXTENSIONS) {
            for (extension in entry.key) {
                if (name.endsWith(extension)) {
                    return entry.value
                }
            }
        }

        return "file"
    }

    val File.size: Double
        get() = if (!exists()) 0.0 else length().toDouble()

    val File.sizeInKb: Double
        get() = size / 1024

    val File.sizeInMb: Double
        get() = sizeInKb / 1024

    val File.sizeInGb: Double
        get() = sizeInMb / 1024

}