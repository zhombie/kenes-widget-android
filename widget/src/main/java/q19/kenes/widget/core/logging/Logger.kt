package q19.kenes.widget.core.logging

import android.util.Log

internal object Logger {

    private const val LIMIT = 4000

    fun debug(tag: String, message: String) {
        if (message.length > LIMIT) {
            Log.d(tag, message.substring(0, LIMIT))
            debug(tag, message.substring(LIMIT))
        } else {
            Log.d(tag, message)
        }
    }

    fun error(tag: String, message: String) {
        if (message.length > LIMIT) {
            Log.e(tag, message.substring(0, LIMIT))
            debug(tag, message.substring(LIMIT))
        } else {
            Log.e(tag, message)
        }
    }

}