package q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri

data class Image constructor(
    override val id: Long,
    override val uri: Uri,
    override val title: String,
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
    duplicateFile = duplicateFile,
    history = history,
    thumbnail = thumbnail,
    folder = folder
), Media.Visual {

    constructor(
        uri: Uri,
        displayName: String,
        duplicateFile: DuplicateFile
    ) : this(
        id = System.currentTimeMillis() + duplicateFile.file.name.length + duplicateFile.file.length(),
        uri = uri,
        title = duplicateFile.file.name,
        displayName = displayName,
        folder = null,
        duplicateFile = duplicateFile,
        history = null,
        thumbnail = null,
        resolution = null
    )

}