package q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri

/**
 * [duration] - The duration time of the [Video]
 */
data class Video constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String,
    override val displayName: String,
    override val duplicateFile: DuplicateFile?,
    override val history: History?,
    override val thumbnail: Bitmap?,
    override val folder: Folder?,

    override val duration: Long,

    override val resolution: Resolution?
) : Media(
    id = id,
    uri = uri,
    title = title,
    displayName = displayName,
    duplicateFile = duplicateFile,
    history = history,
    thumbnail = thumbnail,
    folder = folder
), Media.Playable, Media.Visual