package q19.kenes.widget.domain.model

import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import java.time.format.DateTimeFormatter

fun Message.hasOnlyTextMessage(): Boolean {
    return !text.isNullOrBlank() &&
        keyboard == null &&
        (media == null || media?.urlPath.isNullOrBlank()) &&
        attachments.isNullOrEmpty() &&
        location == null
}

fun Message.hasOnlyImageAndTextMessage(): Boolean {
    return hasOnlyMediaAndText(Media.Type.IMAGE)
}

fun Message.hasOnlyVideoAndTextMessage(): Boolean {
    return hasOnlyMediaAndText(Media.Type.VIDEO)
}

fun Message.hasOnlyAudioAndTextMessage(): Boolean {
    return hasOnlyMediaAndText(Media.Type.AUDIO)
}

fun Message.isEmpty(): Boolean {
    return title.isNullOrBlank() &&
        text.isNullOrBlank() &&
        keyboard == null &&
        (media == null || media?.urlPath.isNullOrBlank()) &&
        attachments.isNullOrEmpty() &&
        location == null
}

fun Message.hasUrlPath(type: Media.Type): Boolean {
    return media != null && !media?.urlPath.isNullOrBlank() && media?.type == type
}

fun Message.hasLocalFile(type: Media.Type): Boolean {
    return media != null && media?.file?.exists == true && media?.type == type
}

fun Message.hasOnlyMediaAndText(type: Media.Type): Boolean {
    return keyboard == null &&
        (hasUrlPath(type) || hasLocalFile(type)) &&
        attachments.isNullOrEmpty() &&
        location == null
}