package q19.kenes_widget.util

import android.os.Environment

internal object SDCardUtil {

    // Check If SD Card is present or not method
    val isSDCardPresent: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

}