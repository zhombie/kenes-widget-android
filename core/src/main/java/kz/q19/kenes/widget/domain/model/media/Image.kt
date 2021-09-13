package kz.q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri

internal data class Image constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String?,
    override val displayName: String,
    override val folder: Folder?,
    override val duplicateFile: DuplicateFile?,
    override val history: History?,
    override val thumbnail: Bitmap?,

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
), Media.Visual {

    constructor(
        uri: Uri,
        displayName: String
    ) : this(
        id = generateId(),
        uri = uri,
        title = null,
        displayName = displayName,
        folder = null,
        duplicateFile = null,
        history = null,
        thumbnail = null,
        resolution = null
    )

    constructor(
        uri: Uri,
        title: String,
        displayName: String,
        duplicateFile: DuplicateFile,
        history: History
    ) : this(
        id = generateId(),
        uri = uri,
        title = title,
        displayName = displayName,
        folder = null,
        duplicateFile = duplicateFile,
        history = history,
        thumbnail = null,
        resolution = null
    )

}