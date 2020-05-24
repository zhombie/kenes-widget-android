package q19.kenes_widget.util

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat

internal object PhoneStorageUtil {

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

}