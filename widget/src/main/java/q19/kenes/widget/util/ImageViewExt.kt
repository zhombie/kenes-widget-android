package q19.kenes.widget.util

import android.net.Uri
import android.widget.ImageView
import q19.kenes.widget.core.Settings

internal fun ImageView?.loadSmallImage(url: String?) {
    loadSmallImage(url.toUri())
}

internal fun ImageView?.loadSmallImage(uri: Uri?) {
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