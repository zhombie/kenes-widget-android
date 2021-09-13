package kz.q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri

internal open class Media constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String?,
    override val displayName: String,
    override val folder: Folder?,
    override val duplicateFile: DuplicateFile?,
    override val history: History?,
    override val thumbnail: Bitmap?
) : Content(
    id = id,
    uri = uri,
    title = title,
    displayName = displayName,
    folder = folder,
    duplicateFile = duplicateFile,
    history = history,
    thumbnail = thumbnail
) {

    interface Playable {
        val duration: Long
    }

    interface Visual {
        val resolution: Resolution?
    }

}