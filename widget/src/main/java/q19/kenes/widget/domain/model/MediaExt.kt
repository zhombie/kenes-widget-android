package q19.kenes.widget.domain.model

import android.net.Uri
import kz.q19.domain.model.media.Media
import q19.kenes.widget.util.UrlUtil
import java.io.File

sealed class Source constructor(
    open val uri: Uri,
    open val displayName: String
) {
    data class LocalFile constructor(
        val file: File,
        override val uri: Uri,
        override val displayName: String
    ) : Source(uri, displayName)

    data class URL constructor(
        val fullUrl: String,
        override val uri: Uri,
        override val displayName: String
    ) : Source(uri, displayName)
}


val Media.source: Source?
    get() {
        val file = file?.get()
        return if (file?.exists() == true) {
            Source.LocalFile(
                file = file,
                uri = Uri.fromFile(file),
                displayName = title ?: file.name
            )
        } else {
            val fullUrl = UrlUtil.buildUrl(urlPath)
            if (!fullUrl.isNullOrBlank()) {
                Source.URL(
                    fullUrl = fullUrl,
                    uri = Uri.parse(fullUrl),
                    displayName = title ?: fullUrl.split("/").last()
                )
            } else {
                null
            }
        }
    }


val Media.sourceUri: Uri?
    get() = when (val source = source) {
        is Source.LocalFile, is Source.URL -> {
            source.uri
        }
        else -> {
            null
        }
    }


fun Media.hasValidUrl(): Boolean {
    return !UrlUtil.buildUrl(urlPath).isNullOrBlank()
}