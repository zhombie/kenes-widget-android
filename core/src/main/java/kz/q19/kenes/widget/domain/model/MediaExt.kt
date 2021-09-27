package kz.q19.kenes.widget.domain.model

import android.net.Uri
import kz.q19.domain.model.media.Media
import kz.q19.kenes.widget.core.URLManager

internal sealed class Source private constructor(
    open val uri: Uri,
    open val title: String
) {
    data class LocalFile constructor(
        override val uri: Uri,
        override val title: String
    ) : Source(uri, title)

    data class URL constructor(
        override val uri: Uri,
        override val title: String
    ) : Source(uri, title)
}


internal val Media.source: Source?
    get() {
        val file = file?.get()
        return if (file?.exists() == true) {
            Source.LocalFile(
                uri = Uri.fromFile(file),
                title = title ?: file.name
            )
        } else {
            val url = URLManager.buildStaticUrl(urlPath)
            if (url.isNullOrBlank()) {
                null
            } else {
                Source.URL(
                    uri = Uri.parse(url),
                    title = title ?: url.split("/").last()
                )
            }
        }
    }


internal val Media.sourceUri: Uri?
    get() = when (val source = source) {
        is Source.LocalFile, is Source.URL -> source.uri
        else -> null
    }


internal fun Media.hasValidUrl(): Boolean {
    return !URLManager.buildStaticUrl(urlPath).isNullOrBlank()
}