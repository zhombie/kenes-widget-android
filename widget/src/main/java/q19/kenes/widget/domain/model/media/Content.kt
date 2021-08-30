package q19.kenes.widget.domain.model.media

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

/**
 * [id] - The unique ID of the [Content]
 * [uri] - The uri path of the [Content] (usually content://...)
 * [title] - The title of the [Content]
 * [displayName] - The display name of the [Content]. For example, an [Content] stored at
 * {@code /storage/0000-0000/DCIM/Vacation/IMG1024.JPG} would have a display name of {@code IMG1024.JPG}.
 * [thumbnail] - The thumbnail/cover image of the [Content]
 * [folder] - The primary folder of this [Content]
 */
open class Content constructor(
    open val id: Long,
    open val uri: Uri,
    open val title: String,
    open val displayName: String,
    open val duplicateFile: DuplicateFile?,
    open val history: History?,
    open val thumbnail: Bitmap?,
    open val folder: Folder?
) {

    /**
     * [mimeType] - The MIME type of the [Content]
     * [extension] - The extension of [Content]
     * [file] - The local duplicate file, which is generated by given [uri]
     */
    data class DuplicateFile constructor(
        val mimeType: String?,
        val extension: String?,
        val file: File
    )

    /**
     * [addedAt] - The time the [Content] was first added (milliseconds)
     * [modifiedAt] - The time the [Content] was last modified (milliseconds)
     * [createdAt] - The time the [Content] was created. If image or video, it is as same as date taken (milliseconds)
     */
    data class History constructor(
        val addedAt: Long?,
        val modifiedAt: Long?,
        val createdAt: Long?,
    )

    override fun toString(): String {
        return "${Content::class.java.simpleName}(id=$id, uri=$uri, file=$duplicateFile, title=$title, displayName=$displayName, thumbnail=$thumbnail, folder=$folder)"
    }

}