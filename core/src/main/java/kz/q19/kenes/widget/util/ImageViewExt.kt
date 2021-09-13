package kz.q19.kenes.widget.util

import android.net.Uri
import android.widget.ImageView
import kz.q19.kenes.widget.core.Settings
import kz.q19.kenes.widget.core.logging.Logger

private const val TAG = "ImageViewExt"

internal fun ImageView?.loadSmallImage(url: String?) {
    loadSmallImage(url.toUri())
}

internal fun ImageView?.loadSmallImage(uri: Uri?) {
    Logger.debug(TAG, "loadSmallImage() -> $this, $uri")
    if (this == null || uri == null) {
        // Ignored
    } else {
        Settings.getImageLoader().loadSmallImage(context, this, uri)
    }
}

internal fun ImageView?.loadStandardImage(url: String?) {
    loadStandardImage(url.toUri())
}

internal fun ImageView?.loadStandardImage(uri: Uri?) {
    if (this == null || uri == null) {
        // Ignored
    } else {
        Settings.getImageLoader().loadStandardImage(context, this, uri)
    }
}

internal fun ImageView?.loadFullscreenImage(url: String?) {
    loadFullscreenImage(url.toUri())
}

internal fun ImageView?.loadFullscreenImage(uri: Uri?) {
    if (this == null || uri == null) {
        // Ignored
    } else {
        Settings.getImageLoader().loadFullscreenImage(context, this, uri)
    }
}

private fun String?.toUri(): Uri? {
    if (isNullOrBlank()) return null
    return Uri.parse(this)
}