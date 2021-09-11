package q19.kenes.widget.domain.model

import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message

private const val TAG = "MessagesExt"

internal fun Message.isEmpty(): Boolean {
    return title.isNullOrBlank() &&
        text.isNullOrBlank() &&
        keyboard == null &&
        (media == null || media?.urlPath.isNullOrBlank()) &&
        attachments.isNullOrEmpty() &&
        location == null
}

internal fun Message.hasOnlyTextMessage(): Boolean {
    return !text.isNullOrBlank() &&
        keyboard == null &&
        (media == null || media?.urlPath.isNullOrBlank()) &&
        attachments.isNullOrEmpty() &&
        location == null
}

internal fun Message.hasOnlyImageMessage(): Boolean {
    return hasOnlyMediaOrAttachments(Media.Type.IMAGE)
}

internal fun Message.hasOnlyImageAlbumMessage(): Boolean {
    return hasOnlyAttachments(Media.Type.IMAGE, 2)
}

internal fun Message.hasOnlyVideoMessage(): Boolean {
    return hasOnlyMediaOrAttachments(Media.Type.VIDEO)
}

internal fun Message.hasOnlyAudioMessage(): Boolean {
    return hasOnlyMediaOrAttachments(Media.Type.AUDIO)
}

internal fun Media.hasRemoteUrlOrLocalFile(): Boolean {
    return hasRemoteUrl() || hasLocalFile()
}

internal fun Media.hasRemoteUrl(): Boolean {
    return !urlPath.isNullOrBlank()
}

internal fun Message.hasMedia(type: Media.Type): Boolean {
    val media = media ?: return false
    return media.type == type && media.hasRemoteUrlOrLocalFile()
}

internal fun Message.hasAttachments(type: Media.Type, atLeast: Int = -1): Boolean {
    val attachments = attachments
    if (attachments.isNullOrEmpty()) return false
    if (attachments.all { it.type == type && it.hasRemoteUrlOrLocalFile() }) {
        if (atLeast == -1) {
            return true
        } else {
            if (attachments.size >= atLeast) {
                return true
            }
        }
    }
    return false
}

internal fun Message.hasOnlyMediaOrAttachments(type: Media.Type): Boolean {
    if (keyboard == null && location == null) {
        if (hasMedia(type)) return true
        if (hasAttachments(type)) return true
    }
    return false
}

internal fun Message.hasOnlyAttachments(type: Media.Type, atLeast: Int = -1): Boolean {
    if (keyboard == null && location == null) {
        if (hasAttachments(type, atLeast)) return true
    }
    return false
}