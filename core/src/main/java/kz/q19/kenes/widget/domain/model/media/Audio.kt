package kz.q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri

/**
 * [duration] - The duration time of the [Audio]
 */
internal data class Audio constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String,
    override val displayName: String,
    override val duplicateFile: DuplicateFile?,
    override val history: History?,
    override val thumbnail: Bitmap?,
    override val folder: Folder?,

    override val duration: Long,

    val album: Album?,
) : Content(
    id = id,
    uri = uri,
    title = title,
    displayName = displayName,
    duplicateFile = duplicateFile,
    history = history,
    thumbnail = thumbnail,
    folder = folder
), Media.Playable {

    data class Album constructor(
        val id: Long,
        val title: String?,
        val artist: String?
    )

}