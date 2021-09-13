package kz.q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri

/**
 * [duration] - The duration time of the [Video]
 */
internal data class Video constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String?,
    override val displayName: String,
    override val folder: Folder?,
    override val duplicateFile: DuplicateFile?,
    override val history: History?,
    override val thumbnail: Bitmap?,

    override val duration: Long,

    override val resolution: Resolution?
) : Media(
    id = id,
    uri = uri,
    title = title,
    displayName = displayName,
    folder = folder,
    duplicateFile = duplicateFile,
    history = history,
    thumbnail = thumbnail
), Media.Playable, Media.Visual {

    constructor(
        uri: Uri,
        displayName: String,
        duplicateFile: DuplicateFile
    ) : this(
        id = generateId(),
        uri = uri,
        title = null,
        displayName = displayName,
        folder = null,
        duplicateFile = duplicateFile,
        history = null,
        thumbnail = null,
        duration = -1,
        resolution = null
    )

}