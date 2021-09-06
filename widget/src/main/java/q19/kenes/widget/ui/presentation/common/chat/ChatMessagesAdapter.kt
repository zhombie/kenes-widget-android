package q19.kenes.widget.ui.presentation.common.chat

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.domain.model.keyboard.button.Button
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.utils.view.inflate
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.domain.model.*
import q19.kenes.widget.ui.presentation.common.chat.viewholder.*

internal class ChatMessagesAdapter constructor(
    var callback: Callback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ChatMessagesAdapter::class.java.simpleName
    }

    object ViewType {
        const val OUTGOING_TEXT_MESSAGE: Int = 100
        const val OUTGOING_IMAGE_MESSAGE: Int = 101
        const val OUTGOING_IMAGE_ALBUM_MESSAGE: Int = 102
        const val OUTGOING_VIDEO_MESSAGE: Int = 103
        const val OUTGOING_AUDIO_MESSAGE: Int = 104
        const val OUTGOING_RICH_CONTENT_MESSAGE: Int = 105

        const val INCOMING_TEXT_MESSAGE: Int = 110
        const val INCOMING_IMAGE_MESSAGE: Int = 111
        const val INCOMING_IMAGE_ALBUM_MESSAGE: Int = 112
        const val INCOMING_VIDEO_MESSAGE: Int = 113
        const val INCOMING_AUDIO_MESSAGE: Int = 114
        const val INCOMING_RICH_CONTENT_MESSAGE: Int = 115

        const val NOTIFICATION: Int = 120
    }

    private object PayloadKey {
        const val ACTION: String = "action"

        const val PROGRESS: String = "progress"

        const val IS_PLAYING: String = "is_playing"

        const val CURRENT_POSITION: String = "current_position_millis"
        const val DURATION: String = "duration_millis"
    }

    private object Action {
        const val SET_DOWNLOAD_PROGRESS: String = "set_download_progress"

        const val SET_AUDIO_PLAY_PROGRESS: String = "set_audio_play_progress"

        const val SET_AUDIO_PLAYBACK_STATE: String = "set_audio_playback_state"
        const val RESET_AUDIO_PLAYBACK_STATE: String = "reset_audio_playback_state"
    }

    private val messages: MutableList<Message> = mutableListOf()

    fun addNewMessage(message: Message, notify: Boolean = true): Boolean {
        messages.add(0, message)
        val isAdded = messages.contains(message)
        if (notify) {
            notifyItemInserted(0)
        }
        return isAdded
    }

    fun setAudioPlaybackState(itemPosition: Int, isPlaying: Boolean) {
        Logger.debug(TAG, "setAudioPlaybackState() -> " +
            "position: $itemPosition, isPlaying: $isPlaying")

        notifyItemChanged(itemPosition, Bundle().apply {
            putString(PayloadKey.ACTION, Action.SET_AUDIO_PLAYBACK_STATE)

            putBoolean(PayloadKey.IS_PLAYING, isPlaying)
        })
    }

    fun resetAudioPlaybackState(itemPosition: Int, duration: Long) {
        Logger.debug(TAG, "resetAudioPlaybackState() -> " +
            "itemPosition: $itemPosition, duration: $duration")

        notifyItemChanged(itemPosition, Bundle().apply {
            putString(PayloadKey.ACTION, Action.RESET_AUDIO_PLAYBACK_STATE)

            putLong(PayloadKey.DURATION, duration)
        })
    }

    fun setAudioPlayProgress(itemPosition: Int, progress: Float, currentPosition: Long, duration: Long) {
        Logger.debug(TAG, "setAudioPlayProgress() -> " +
            "itemPosition: $itemPosition, " +
            "progress: $progress, " +
            "currentPosition: $currentPosition, " +
            "duration: $duration")

        notifyItemChanged(itemPosition, Bundle().apply {
            putString(PayloadKey.ACTION, Action.SET_AUDIO_PLAY_PROGRESS)

            putFloat(PayloadKey.PROGRESS, progress)
            putLong(PayloadKey.CURRENT_POSITION, currentPosition)
            putLong(PayloadKey.DURATION, duration)
        })
    }

    private fun getItem(position: Int): Message = messages[position]

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages.isNotEmpty()) {
            val message = getItem(position)
            when (message.type) {
                Message.Type.OUTGOING -> {
                    when {
                        message.hasOnlyTextMessage() ->
                            ViewType.OUTGOING_TEXT_MESSAGE
                        message.hasOnlyImageAlbumMessage() ->
                            ViewType.OUTGOING_IMAGE_ALBUM_MESSAGE
                        message.hasOnlyImageMessage() ->
                            ViewType.OUTGOING_IMAGE_MESSAGE
                        message.hasOnlyVideoMessage() ->
                            ViewType.OUTGOING_VIDEO_MESSAGE
                        message.hasOnlyAudioMessage() ->
                            ViewType.OUTGOING_AUDIO_MESSAGE
                        else ->
                            ViewType.OUTGOING_RICH_CONTENT_MESSAGE
                    }
                }
                Message.Type.INCOMING -> {
                    when {
                        message.hasOnlyTextMessage() ->
                            ViewType.INCOMING_TEXT_MESSAGE
                        message.hasOnlyImageAlbumMessage() ->
                            ViewType.INCOMING_IMAGE_ALBUM_MESSAGE
                        message.hasOnlyImageMessage() ->
                            ViewType.INCOMING_IMAGE_MESSAGE
                        message.hasOnlyVideoMessage() ->
                            ViewType.INCOMING_VIDEO_MESSAGE
                        message.hasOnlyAudioMessage() ->
                            ViewType.INCOMING_AUDIO_MESSAGE
                        else ->
                            ViewType.INCOMING_RICH_CONTENT_MESSAGE
                    }
                }
                Message.Type.NOTIFICATION ->
                    ViewType.NOTIFICATION
            }
        } else {
            super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OUTGOING_TEXT_MESSAGE ->
                OutgoingTextMessageViewHolder(
                    parent.inflate(OutgoingTextMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.OUTGOING_IMAGE_ALBUM_MESSAGE ->
                OutgoingImageAlbumMessageViewHolder(
                    parent.inflate(OutgoingImageAlbumMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.OUTGOING_IMAGE_MESSAGE ->
                OutgoingImageMessageViewHolder(
                    parent.inflate(OutgoingImageMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.OUTGOING_VIDEO_MESSAGE ->
                OutgoingVideoMessageViewHolder(
                    parent.inflate(OutgoingVideoMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.OUTGOING_AUDIO_MESSAGE ->
                OutgoingAudioMessageViewHolder(
                    parent.inflate(OutgoingAudioMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.OUTGOING_RICH_CONTENT_MESSAGE ->
                OutgoingRichContentMessageViewHolder(
                    parent.inflate(OutgoingRichContentMessageViewHolder.LAYOUT),
                    callback
                )

            ViewType.INCOMING_TEXT_MESSAGE ->
                IncomingTextMessageViewHolder(
                    parent.inflate(IncomingTextMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.INCOMING_IMAGE_ALBUM_MESSAGE ->
                IncomingImageAlbumMessageViewHolder(
                    parent.inflate(IncomingImageAlbumMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.INCOMING_IMAGE_MESSAGE ->
                IncomingImageMessageViewHolder(
                    parent.inflate(IncomingImageMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.INCOMING_VIDEO_MESSAGE ->
                IncomingVideoMessageViewHolder(
                    parent.inflate(IncomingVideoMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.INCOMING_AUDIO_MESSAGE ->
                IncomingAudioMessageViewHolder(
                    parent.inflate(IncomingAudioMessageViewHolder.LAYOUT),
                    callback
                )
            ViewType.INCOMING_RICH_CONTENT_MESSAGE ->
                IncomingRichContentMessageViewHolder(
                    parent.inflate(IncomingRichContentMessageViewHolder.LAYOUT),
                    callback
                )

            ViewType.NOTIFICATION ->
                NotificationMessageViewHolder(parent.inflate(NotificationMessageViewHolder.LAYOUT))

            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        Logger.debug(TAG, "onBindViewHolder() -> holder: $holder, message: $message")

        when (holder) {
            is OutgoingTextMessageViewHolder -> holder.bind(message)
            is OutgoingImageAlbumMessageViewHolder -> holder.bind(message)
            is OutgoingImageMessageViewHolder -> holder.bind(message)
            is OutgoingVideoMessageViewHolder -> holder.bind(message)
            is OutgoingAudioMessageViewHolder -> holder.bind(message)
            is OutgoingRichContentMessageViewHolder -> holder.bind(message)

            is IncomingTextMessageViewHolder -> holder.bind(message)
            is IncomingImageAlbumMessageViewHolder -> holder.bind(message)
            is IncomingImageMessageViewHolder -> holder.bind(message)
            is IncomingVideoMessageViewHolder -> holder.bind(message)
            is IncomingAudioMessageViewHolder -> holder.bind(message)
            is IncomingRichContentMessageViewHolder -> holder.bind(message)

            is NotificationMessageViewHolder -> holder.bind(message)

            else -> {
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val payload = payloads.lastOrNull()

//        debug(TAG, "onBindViewHolder() -> payload: $payload")

        if (payload is Bundle) {
            if (holder is IncomingAudioMessageViewHolder) {
                val message = getItem(position)

                when (payload.getString(PayloadKey.ACTION)) {
                    Action.SET_AUDIO_PLAYBACK_STATE ->
                        holder.setAudioPlaybackState(payload.getBoolean(PayloadKey.IS_PLAYING))
                    Action.RESET_AUDIO_PLAYBACK_STATE ->
                        holder.resetAudioPlaybackState(payload.getLong(PayloadKey.DURATION))

                    Action.SET_AUDIO_PLAY_PROGRESS ->
                        holder.setAudioPlayProgress(
                            currentPosition = payload.getLong(PayloadKey.CURRENT_POSITION),
                            duration = payload.getLong(PayloadKey.DURATION),
                            progress = payload.getFloat(PayloadKey.PROGRESS)
                        )
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    interface Callback {
        // Text
        fun onUrlInTextClicked(url: String) {}

        // Multimedia
        fun onImageClicked(imageView: ImageView, media: Media) {}
        fun onImagesClicked(recyclerView: RecyclerView, images: List<Media>, imagePosition: Int) {}
        fun onVideoClicked(imageView: ImageView, media: Media) {}
        fun onAudioClicked(media: Media, itemPosition: Int) {}
        fun onMediaClicked(media: Media, itemPosition: Int) {}

        // Button
        fun onInlineButtonClicked(message: Message, button: Button, itemPosition: Int) {}

        // Seek bar
        fun onSeekBarChange(media: Media, progress: Int): Boolean = false

        // Message
        fun onMessageLongClicked(text: String) {}
    }
}