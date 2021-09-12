package kz.q19.kenes.widget.domain.model.media

import android.net.Uri

internal data class Folder constructor(
    val id: Long,
    val displayName: String? = null,
    val items: List<Content> = emptyList()
) {

    val cover: Uri?
        get() = if (items.isEmpty()) null else items.first().uri

    val itemsCount: Int
        get() = items.size

}
