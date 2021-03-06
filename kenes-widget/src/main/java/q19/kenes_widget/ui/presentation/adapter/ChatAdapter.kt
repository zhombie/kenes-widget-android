package q19.kenes_widget.ui.presentation.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.data.model.Attachment
import q19.kenes_widget.data.model.Category
import q19.kenes_widget.data.model.Media
import q19.kenes_widget.data.model.Message
import q19.kenes_widget.ui.components.base.HtmlTextView
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.inflate
import q19.kenes_widget.util.loadRoundedImage
import q19.kenes_widget.util.removeCompoundDrawables
import q19.kenes_widget.util.showPendingFileDownloadAlert
import java.util.concurrent.TimeUnit

class ChatAdapter(
    var callback: Callback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "ChatAdapter"

        private val LAYOUT_OURGOING_MESSAGE = R.layout.kenes_cell_outgoing_message
        private val LAYOUT_INCOMING_MESSAGE = R.layout.kenes_cell_incoming_message
        private val LAYOUT_MESSAGE_KEYBOARD = R.layout.kenes_cell_message_keyboard
        val LAYOUT_NOTIFICATION = R.layout.kenes_cell_notification
        private val LAYOUT_TYPING = R.layout.kenes_cell_typing
        private val LAYOUT_CATEGORY = R.layout.kenes_cell_category
        private val LAYOUT_CROSS_CHILDREN = R.layout.kenes_cell_cross_children
        private val LAYOUT_RESPONSE = R.layout.kenes_cell_response

        private const val KEY_PROGRESS = "progress"
        private const val KEY_FILE_TYPE = "fileType"
        private const val KEY_START_TIME = "startTime"
        private const val KEY_END_TIME = "endTime"
        private const val KEY_ACTION = "action"
        private const val KEY_CURRENT_POSITION_MILLIS = "currentPositionMillis"
        private const val KEY_DURATION_MILLIS = "durationMillis"
    }

    private var messages = mutableListOf<Message>()

    fun addNewMessage(message: Message, isNotifyEnabled: Boolean = true) {
        debug(TAG, "addNewMessage -> message: $message")

        messages.add(message)

        if (isNotifyEnabled) {
            notifyItemInserted(messages.size - 1)
        }
    }

    fun setNewMessages(message: Message, isNotifyEnabled: Boolean = true) {
        debug(TAG, "setNewMessages: $message")
        setNewMessages(listOf(message), isNotifyEnabled)
    }

    fun setNewMessages(messages: List<Message>, isNotifyEnabled: Boolean = true) {
        debug(TAG, "setNewMessages: $messages")

        if (messages.isEmpty()) return

        if (this.messages.isNotEmpty()) {
            this.messages.clear()
        }

        this.messages.addAll(messages)

        if (isNotifyEnabled) {
            notifyDataSetChanged()
        }
    }

    fun clear(isNotifyEnabled: Boolean = true) {
        if (messages.isEmpty()) return

        messages.clear()

        if (isNotifyEnabled) {
            notifyDataSetChanged()
        }
    }

    fun clearCategoryMessages(isNotifyEnabled: Boolean = true): Boolean {
        val isRemoved = messages.removeAll { it.category != null }
        if (isRemoved && isNotifyEnabled) {
            notifyDataSetChanged()
        }
        return isRemoved
    }

    fun setDownloading(downloadStatus: Message.File.DownloadStatus, itemPosition: Int) {
        getItem(itemPosition).file.downloadStatus = downloadStatus
        notifyItemChanged(itemPosition)
    }

    fun setAudioStartTime(startTime: Int, itemPosition: Int) {
        debug(TAG, "setAudioStartTime -> startTime: $startTime, itemPosition: $itemPosition")

        notifyItemChanged(itemPosition, Bundle().apply {
            putInt(KEY_START_TIME, startTime)
            putString(KEY_ACTION, "setStartTime")
            putString(KEY_FILE_TYPE, "audio")
        })
    }

    fun setAudioEndTime(endTime: Int, itemPosition: Int) {
        debug(TAG, "setAudioEndTime -> endTime: $endTime, itemPosition: $itemPosition")

        notifyItemChanged(itemPosition, Bundle().apply {
            putInt(KEY_END_TIME, endTime)
            putString(KEY_ACTION, "setEndTime")
            putString(KEY_FILE_TYPE, "audio")
        })
    }

    fun setAudioPaused(itemPosition: Int) {
        debug(TAG, "setAudioPaused -> position: $itemPosition")

        notifyItemChanged(itemPosition, Bundle().apply {
            putString(KEY_ACTION, "pauseAudio")
        })
    }

    fun setAudioProgress(progress: Int, currentPosition: Int, duration: Int, itemPosition: Int) {
//        debug(TAG, "setAudioProgress -> progress: $progress, currentPosition: $currentPosition, duration: $duration, itemPosition: $itemPosition")

        notifyItemChanged(itemPosition, Bundle().apply {
            putInt(KEY_PROGRESS, progress)
            putInt(KEY_CURRENT_POSITION_MILLIS, currentPosition)
            putInt(KEY_DURATION_MILLIS, duration)
            putString(KEY_FILE_TYPE, "audio")
        })
    }

    fun setProgress(progress: Int, fileType: String, itemPosition: Int) {
//        debug(TAG, "setProgress -> progress: $progress, fileType: $fileType, itemPosition: $itemPosition")

        getItem(itemPosition).apply {
            file.progress = progress
            file.type = fileType
        }
        notifyItemChanged(itemPosition, Bundle().apply {
            putInt(KEY_PROGRESS, progress)
            putString(KEY_FILE_TYPE, fileType)
        })
    }

    private fun getItem(position: Int) = messages[position]

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages.isNotEmpty()) {
            val item = getItem(position)
            when (item.type) {
                Message.Type.OUTGOING ->
                    LAYOUT_OURGOING_MESSAGE
                Message.Type.INCOMING -> {
                    if (item.replyMarkup != null) {
                        LAYOUT_MESSAGE_KEYBOARD
                    } else {
                        LAYOUT_INCOMING_MESSAGE
                    }
                }
                Message.Type.NOTIFICATION ->
                    LAYOUT_NOTIFICATION
                Message.Type.TYPING ->
                    LAYOUT_TYPING
                Message.Type.CATEGORY ->
                    LAYOUT_CATEGORY
                Message.Type.CROSS_CHILDREN ->
                    LAYOUT_CROSS_CHILDREN
                Message.Type.RESPONSE ->
                    LAYOUT_RESPONSE
            }
        } else {
            super.getItemViewType(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = parent.inflate(viewType)
        return when (viewType) {
            LAYOUT_OURGOING_MESSAGE -> OutgoingMessageViewHolder(view)
            LAYOUT_INCOMING_MESSAGE -> IncomingMessageViewHolder(view)
            LAYOUT_MESSAGE_KEYBOARD -> MessageKeyboardViewHolder(view)
            LAYOUT_NOTIFICATION -> NotificationViewHolder(view)
            LAYOUT_TYPING -> TypingViewHolder(view)
//            LAYOUT_CATEGORY -> OldCategoryViewHolder(view)
            LAYOUT_CATEGORY -> CategoryViewHolder(view)
            LAYOUT_CROSS_CHILDREN -> CrossChildrenViewHolder(view)
            LAYOUT_RESPONSE -> ResponseViewHolder(view)
            else -> throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (message.type) {
            Message.Type.OUTGOING ->
                if (holder is OutgoingMessageViewHolder) holder.bind(message)
            Message.Type.INCOMING -> {
                if (message.replyMarkup != null) {
                    if (holder is MessageKeyboardViewHolder) holder.bind(message)
                } else {
                    if (holder is IncomingMessageViewHolder) holder.bind(message)
                }
            }
            Message.Type.NOTIFICATION ->
                if (holder is NotificationViewHolder) holder.bind(message)
            Message.Type.TYPING ->
                if (holder is TypingViewHolder) holder.bind()
            Message.Type.CATEGORY ->
//                if (holder is OldCategoryViewHolder) holder.bind(message)
                if (holder is CategoryViewHolder) holder.bind(message)
            Message.Type.CROSS_CHILDREN ->
                if (holder is CrossChildrenViewHolder) holder.bind(message)
            Message.Type.RESPONSE ->
                if (holder is ResponseViewHolder) holder.bind(message)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val payload = payloads.lastOrNull()
        if (payload != null) {
            if (holder is IncomingMessageViewHolder && payload is Bundle) {
                val message = getItem(position)
                val fileType = payload.getString(KEY_FILE_TYPE)
                val progress = payload.getInt(KEY_PROGRESS)

                debug(TAG, "onBindViewHolder -> fileType: $fileType, progress: $progress")

                if (fileType == "media") {
                    holder.setMediaDownloadProgress(
                        message.media,
                        message.file.downloadStatus,
                        payload.getInt(KEY_PROGRESS)
                    )
                } else if (fileType == "audio") {
                    when (payload.getString(KEY_ACTION)) {
                        "setStartTime" ->
                            holder.setAudioStartTime(payload.getInt(KEY_START_TIME))
                        "setEndTime" ->
                            holder.setAudioEndTime(payload.getInt(KEY_END_TIME))
                        "pauseAudio" ->
                            holder.setAudioPaused()
                        else -> holder.setAudioPlayProgress(
                            payload.getInt(KEY_CURRENT_POSITION_MILLIS),
                            payload.getInt(KEY_DURATION_MILLIS),
                            progress
                        )
                    }
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private inner class OutgoingMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ImageView>(R.id.imageView)
        private val mediaView = view.findViewById<LinearLayout>(R.id.mediaView)
        private val iconView = view.findViewById<ImageView>(R.id.iconView)
        private val mediaNameView = view.findViewById<TextView>(R.id.mediaNameView)
        private val textView = view.findViewById<HtmlTextView>(R.id.textView)
        private val timeView = view.findViewById<TextView>(R.id.timeView)

        init {
            timeView.visibility = View.GONE
        }

        fun bind(message: Message) {
            val context = itemView.context

            val media = message.media
            if (media == null) {
                imageView?.visibility = View.GONE
                mediaView?.visibility = View.GONE
            } else {
                if (media.isImage) {
                    imageView?.visibility = View.VISIBLE

                    imageView?.loadRoundedImage(
                        media.imageUrl,
                        itemView.resources.getDimensionPixelOffset(R.dimen.kenes_message_background_corner_radius)
                    )

                    imageView?.setOnClickListener {
                        callback?.onImageClicked(
                            imageView,
                            media.imageUrl ?: return@setOnClickListener
                        )
                    }

                    timeView?.text = message.time
                    timeView?.visibility = View.VISIBLE
                } else {
                    imageView?.visibility = View.GONE
                }

                if (media.isFile) {
                    val isEmptyMediaName = context.bindMedia(media)

                    mediaView?.setOnClickListener {
                        if (message.file.type == "media" && message.file.downloadStatus == Message.File.DownloadStatus.PENDING) {
                            context.showPendingFileDownloadAlert {}
                            return@setOnClickListener
                        }
                        callback?.onMediaClicked(media, absoluteAdapterPosition)
                    }

                    if (!isEmptyMediaName) {
                        mediaView?.visibility = View.VISIBLE

                        timeView?.text = message.time
                        timeView?.visibility = View.VISIBLE
                    } else {
                        mediaView?.visibility = View.GONE
                    }
                } else {
                    mediaView?.visibility = View.GONE
                }
            }

            if (message.text.isNotBlank()) {
                textView?.text = message.text
                timeView?.text = message.time

                textView?.enableAutoLinkMask()
                textView?.enableLinkMovementMethod()

                textView?.visibility = View.VISIBLE
                timeView?.visibility = View.VISIBLE
            } else {
                textView?.visibility = View.GONE
            }
        }

        private fun Context.bindMedia(media: Media): Boolean {
            var title = media.hash ?: ""

            if (title.length > 25) {
                val lastIndex = title.length
                title = title.substring(0, 10) + "..." + title.substring(lastIndex - 10, lastIndex)
            }

            val spannableStringBuilder = SpannableStringBuilder()
                .append(title)

            if (media.fileTypeStringRes > 0) {
                spannableStringBuilder.append("\n")
                spannableStringBuilder.append("(" + getString(media.fileTypeStringRes) + ")")
                spannableStringBuilder.append(" - ")
            } else {
                spannableStringBuilder.append("\n")
            }

            iconView?.setImageResource(R.drawable.kenes_ic_document_white)

            val spannableString = SpannableString(getString(R.string.kenes_open_file))
            spannableString.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this,
                        R.color.kenes_light_blue
                    )
                ), 0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            spannableStringBuilder.append(spannableString)

            mediaNameView?.text = spannableStringBuilder

            return mediaNameView.text.isBlank()
        }
    }

    private inner class IncomingMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ImageView>(R.id.imageView)
        private val mediaView = view.findViewById<RelativeLayout>(R.id.mediaView)
        private val iconView = view.findViewById<ImageView>(R.id.iconView)
        private val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        private val mediaNameView = view.findViewById<TextView>(R.id.mediaNameView)
        private val mediaPlaySeekBar = view.findViewById<SeekBar>(R.id.mediaPlaySeekBar)
        private val mediaPlayTimeView = view.findViewById<TextView>(R.id.mediaPlayTimeView)
        private val textView = view.findViewById<HtmlTextView>(R.id.textView)
        private val timeView = view.findViewById<TextView>(R.id.timeView)
        private val attachmentView = view.findViewById<TextView>(R.id.attachmentView)

        init {
            timeView?.visibility = View.GONE

            mediaPlaySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        callback?.onStopTrackingTouch(it.progress, absoluteAdapterPosition)
                    }
                }
            })
        }

        fun bind(message: Message) {
            val context = itemView.context

            val media = message.media
            if (media == null) {
                imageView?.visibility = View.GONE
                mediaView?.visibility = View.GONE
            } else {
                if (media.isImage) {
                    imageView?.visibility = View.VISIBLE

                    imageView?.loadRoundedImage(
                        media.imageUrl,
                        itemView.resources.getDimensionPixelOffset(R.dimen.kenes_message_background_corner_radius)
                    )

                    imageView?.setOnClickListener {
                        callback?.onImageClicked(
                            imageView, media.imageUrl ?: return@setOnClickListener
                        )
                    }

                    timeView?.text = message.time
                    timeView?.visibility = View.VISIBLE
                } else {
                    imageView?.visibility = View.GONE
                }

                when {
                    media.isAudio -> {
                        val isBound = bindAudio(message.file.downloadStatus)

                        if (isBound) {
                            mediaNameView?.visibility = View.GONE

                            timeView?.text = message.time
                            timeView?.visibility = View.VISIBLE

                            mediaView?.visibility = View.VISIBLE
                        } else {
                            mediaNameView?.visibility = View.GONE
                            mediaPlaySeekBar?.visibility = View.GONE
                            mediaPlayTimeView?.visibility = View.GONE

                            mediaView?.visibility = View.GONE
                        }

                        mediaView?.setOnClickListener {
                            if (message.file.type == "media" && message.file.downloadStatus == Message.File.DownloadStatus.PENDING) {
                                context.showPendingFileDownloadAlert {}
                                return@setOnClickListener
                            }
                            callback?.onMediaClicked(media, absoluteAdapterPosition)
                        }
                    }
                    media.isFile -> {
                        val isEmptyMediaName = context.bindFile(media, message.file.downloadStatus)

                        mediaView?.setOnClickListener {
                            if (message.file.type == "media" && message.file.downloadStatus == Message.File.DownloadStatus.PENDING) {
                                context.showPendingFileDownloadAlert {}
                                return@setOnClickListener
                            }
                            callback?.onMediaClicked(media, absoluteAdapterPosition)
                        }

                        if (!isEmptyMediaName) {
                            mediaNameView?.visibility = View.VISIBLE

                            timeView?.text = message.time
                            timeView?.visibility = View.VISIBLE

                            mediaView?.visibility = View.VISIBLE
                        } else {
                            mediaNameView?.visibility = View.GONE

                            mediaView?.visibility = View.GONE
                        }

                        mediaPlaySeekBar?.visibility = View.GONE
                        mediaPlayTimeView?.visibility = View.GONE
                    }
                    else -> {
                        mediaNameView?.visibility = View.GONE
                        mediaPlaySeekBar?.visibility = View.GONE
                        mediaPlayTimeView?.visibility = View.GONE

                        mediaView?.visibility = View.GONE
                    }
                }
            }

            if (message.text.isNotBlank()) {
                textView?.setHtmlText(message.htmlText) { _, url ->
                    debug(TAG, "OnClick: $url")
                    callback?.onUrlInTextClicked(url)
                }

                textView?.enableAutoLinkMask()
                textView?.enableLinkMovementMethod()

                timeView?.text = message.time

                textView?.visibility = View.VISIBLE
                timeView?.visibility = View.VISIBLE
            } else {
                textView?.visibility = View.GONE
            }

            val attachments = message.attachments
            if (!attachments.isNullOrEmpty()) {
                val attachment = attachments.first()

                if (attachment.type == "image") {
                    imageView?.loadRoundedImage(
                        attachment.url,
                        itemView.resources.getDimensionPixelOffset(R.dimen.kenes_message_background_corner_radius)
                    )
                    imageView?.visibility = View.VISIBLE

                    imageView?.setOnClickListener {
                        callback?.onImageClicked(
                            imageView, attachment.url ?: return@setOnClickListener
                        )
                    }

                    attachmentView?.text = null
                    attachmentView?.visibility = View.GONE
                } else {
                    attachmentView?.setOnClickListener {
                        if (message.file.type == "attachment" && message.file.downloadStatus == Message.File.DownloadStatus.PENDING) {
                            context.showPendingFileDownloadAlert {}
                            return@setOnClickListener
                        }
                        callback?.onAttachmentClicked(attachment, absoluteAdapterPosition)
                    }

                    imageView?.visibility = View.GONE

                    attachmentView?.text = attachment.title
                    attachmentView?.visibility = View.VISIBLE
                }
            } else {
                attachmentView?.visibility = View.GONE
            }
        }

        fun setMediaDownloadProgress(
            media: Media?,
            downloadStatus: Message.File.DownloadStatus,
            progress: Int
        ) {
            progressBar?.progress = if (progress == 0 || progress == 100) 0 else progress

            if (downloadStatus == Message.File.DownloadStatus.COMPLETED) {
                media?.let { itemView.context.bindFile(it, downloadStatus) }
            }
        }

        fun setAudioStartTime(startTime: Int) {
            debug(TAG, "setAudioStartTime -> startTime: $startTime")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlaySeekBar?.min = startTime
            }
        }

        fun setAudioEndTime(endTime: Int) {
            debug(TAG, "setAudioEndTime -> endTime: $endTime")

            mediaPlaySeekBar?.max = endTime
        }

        fun setAudioPaused() {
            debug(TAG, "setAudioPaused")

            iconView?.setImageResource(R.drawable.kenes_ic_play)
        }

        fun setAudioPlayProgress(currentPosition: Int, duration: Int, progress: Int) {
            debug(TAG, "setAudioPlayProgress -> progress: $progress")

            mediaPlaySeekBar?.let {
                it.progress = progress

                if (it.progress < it.max) {
                    iconView?.setImageResource(R.drawable.kenes_ic_pause)
                } else {
                    iconView?.setImageResource(R.drawable.kenes_ic_play)
                }
            }

            mediaPlayTimeView?.text = formatAudioProgress(currentPosition, duration)
        }

        private fun formatToDigitalClock(milliseconds: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(milliseconds).toInt() % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).toInt() % 60
            return when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
                minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
                seconds > 0 -> String.format("00:%02d", seconds)
                else -> "00:00"
            }
        }

        private fun formatAudioProgress(current: Int, end: Int): String {
            return formatToDigitalClock(current.toLong()) + " / " + formatToDigitalClock(end.toLong())
        }

        private fun bindAudio(downloadStatus: Message.File.DownloadStatus): Boolean {
            if (downloadStatus == Message.File.DownloadStatus.COMPLETED) {
                progressBar?.progress = 0

                iconView?.setImageResource(R.drawable.kenes_ic_play)
            } else {
                iconView?.setImageResource(R.drawable.kenes_ic_download_white)
            }

            mediaPlayTimeView?.text = formatAudioProgress(0, 0)
            mediaPlayTimeView?.visibility = View.VISIBLE

            return true
        }

        private fun Context.bindFile(
            media: Media,
            downloadStatus: Message.File.DownloadStatus
        ): Boolean {
            var title = media.hash ?: ""

            if (title.length > 25) {
                val lastIndex = title.length
                title = title.substring(0, 10) + "..." + title.substring(lastIndex - 10, lastIndex)
            }

            val spannableStringBuilder = SpannableStringBuilder()
                .append(title)

            if (media.fileTypeStringRes > 0) {
                spannableStringBuilder.append("\n")
                spannableStringBuilder.append("(" + getString(media.fileTypeStringRes) + ")")
                spannableStringBuilder.append(" - ")
            } else {
                spannableStringBuilder.append("\n")
            }

            if (downloadStatus == Message.File.DownloadStatus.COMPLETED) {
                progressBar?.progress = 0

                iconView?.setImageResource(R.drawable.kenes_ic_document_white)
                val spannableString = SpannableString(getString(R.string.kenes_open_file))
                spannableString.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            this,
                            R.color.kenes_light_blue
                        )
                    ), 0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                spannableStringBuilder.append(spannableString)
            } else {
                iconView?.setImageResource(R.drawable.kenes_ic_download_white)
                val spannableString = SpannableString(getString(R.string.kenes_file_download))
                spannableString.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            this,
                            R.color.kenes_light_blue
                        )
                    ), 0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                spannableStringBuilder.append(spannableString)
            }

            mediaNameView?.text = spannableStringBuilder

            return mediaNameView?.text.isNullOrBlank()
        }
    }

    private inner class MessageKeyboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<HtmlTextView>(R.id.textView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        private val timeView = view.findViewById<TextView>(R.id.timeView)

        private var inlineKeyboardAdapter: InlineKeyboardAdapter? = null

        private val itemDecoration by lazy {
            InlineKeyboardAdapterItemDecoration(
                itemView.context,
                itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_width),
                itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
            )
        }

        init {
            recyclerView?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        fun bind(message: Message) {
            textView?.setHtmlText(message.htmlText) { _, url ->
                callback?.onUrlInTextClicked(url)
            }

            textView?.enableAutoLinkMask()
            textView?.enableLinkMovementMethod()

            timeView?.text = message.time

            val replyMarkup = message.replyMarkup
            if (replyMarkup != null) {
                if (inlineKeyboardAdapter == null) {
                    inlineKeyboardAdapter = InlineKeyboardAdapter {
                        callback?.onReplyMarkupButtonClicked(it)
                    }
                }

                if (recyclerView?.adapter == null) {
                    recyclerView?.adapter = inlineKeyboardAdapter
                }

                recyclerView?.addItemDecoration(itemDecoration)

                val columnsCount = replyMarkup.getColumnsCount()

                debug(TAG, "columnsCount: $columnsCount")

                val layoutManager = GridLayoutManager(
                    itemView.context,
                    columnsCount,
                    GridLayoutManager.VERTICAL,
                    false
                )

                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val itemCount = layoutManager.itemCount
                        return if (columnsCount > 0 && itemCount % columnsCount > 0 && position == itemCount - 1) {
                            columnsCount
                        } else {
                            1
                        }
                    }
                }

                recyclerView?.layoutManager = layoutManager
            }

            inlineKeyboardAdapter?.replyMarkup = replyMarkup
        }

    }

    private class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<TextView>(R.id.textView)
        private val timeView = view.findViewById<TextView>(R.id.timeView)

        fun bind(message: Message) {
            textView?.text = message.text
            timeView?.text = message.time
        }
    }

    private class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
        }
    }

    private inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view),
        CategoryAdapter.Callback {
        private val titleView = view.findViewById<TextView>(R.id.titleView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private val adapter: CategoryAdapter
        private val layoutManager =
            LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

        init {
            recyclerView?.layoutManager = layoutManager
            adapter = CategoryAdapter(isExpandable = true, isSeparateFooterEnabled = false, callback = this)
            recyclerView?.adapter = adapter

            recyclerView?.addItemDecoration(
                CategoryAdapterItemDecoration(
                    itemView.context,
                    itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_width),
                    itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                )
            )

//            recyclerView?.disableChangeAnimations()
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                adapter.category = category
            }
        }

        override fun onCategoryChildClicked(category: Category) {
            callback?.onCategoryChildClicked(category)
        }

        override fun onGoBackButtonClicked(category: Category) {
            // ignored
        }
    }

    @Deprecated("Old way with horizontal scroll")
    private inner class OldCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view),
        OldCategoryAdapter.Callback {
        private val titleView = view.findViewById<TextView>(R.id.titleView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
//        private val showAllButton = view.findViewById<AppCompatTextView>(R.id.showAllButton)

        private val categoryAdapter: OldCategoryAdapter
        private val layoutManager =
            LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)

        init {
            recyclerView?.layoutManager = layoutManager
            categoryAdapter = OldCategoryAdapter(this)
            recyclerView?.adapter = categoryAdapter
            recyclerView?.addItemDecoration(ItemDecoration(itemView.context))
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                categoryAdapter.category = category
                categoryAdapter.notifyDataSetChanged()

//                showAllButton?.setOnClickListener {
//                    callback?.onShowAllCategoryChildClicked(category)
//                }
            }
        }

        override fun onChildClicked(category: Category) {
            callback?.onCategoryChildClicked(category)
        }

        private inner class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

            private var horizontalSpacing: Int =
                context.resources.getDimensionPixelOffset(R.dimen.kenes_category_horizontal_spacing)

            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)

                outRect.right = horizontalSpacing
            }
        }
    }

    private inner class CrossChildrenViewHolder(view: View) : RecyclerView.ViewHolder(view),
        CategoryAdapter.Callback {
        private val titleView = view.findViewById<TextView>(R.id.titleView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private val adapter: CategoryAdapter
        private val layoutManager =
            LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

        init {
            recyclerView?.layoutManager = layoutManager
            adapter = CategoryAdapter(isExpandable = false, isSeparateFooterEnabled = true, callback = this)
            recyclerView?.adapter = adapter
            recyclerView?.addItemDecoration(
                CategoryAdapterItemDecoration(
                    itemView.context,
                    itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_width),
                    itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                )
            )
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                // Add empty value, in order to make clear, that there is no children
                if (category.children.isNullOrEmpty()) {
                    category.children.add(Category.EMPTY)
                }
                adapter.category = category

                titleView?.setOnClickListener {
                    callback?.onGoBackClicked(category)
                }
            }
        }

        override fun onCategoryChildClicked(category: Category) {
            callback?.onCategoryChildClicked(category)
        }

        override fun onGoBackButtonClicked(category: Category) {
            callback?.onGoBackClicked(category)
        }
    }

    private inner class ResponseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<TextView>(R.id.titleView)
        private val textView = view.findViewById<HtmlTextView>(R.id.textView)
        private val timeView = view.findViewById<TextView>(R.id.timeView)
        private val attachmentView = view.findViewById<TextView>(R.id.attachmentView)
        private val backButtonView = view.findViewById<RelativeLayout>(R.id.backButtonView)
        private val backButton = backButtonView.findViewById<AppCompatTextView>(R.id.textView)
        private val backButtonImageView = view.findViewById<AppCompatImageView>(R.id.imageView)

        fun bind(message: Message) {
            val category = message.category

            if (category != null) {
                if (category.title.isNotBlank()) {
                    titleView?.text = category.title
                    titleView?.visibility = View.VISIBLE

                    titleView?.setOnClickListener {
                        callback?.onGoBackClicked(category)
                    }
                } else {
                    titleView?.visibility = View.GONE
                }

                if (message.text.isNotBlank()) {
                    textView?.setHtmlText(message.htmlText) { _, url ->
                        debug(TAG, "OnClick: $url")
                        callback?.onUrlInTextClicked(url)
                    }

                    timeView?.text = message.time

                    textView?.enableAutoLinkMask()
                    textView?.enableLinkMovementMethod()

                    textView?.visibility = View.VISIBLE
                    timeView?.visibility = View.VISIBLE
                } else {
                    textView?.visibility = View.GONE
                    timeView?.visibility = View.GONE
                }

                val attachments = message.attachments
                if (!attachments.isNullOrEmpty()) {
                    val attachment = attachments[0]

                    attachmentView?.text = attachment.title
                    attachmentView?.visibility = View.VISIBLE

                    attachmentView?.setOnClickListener {
                        callback?.onAttachmentClicked(attachment, absoluteAdapterPosition)
                    }
                } else {
                    attachmentView?.visibility = View.GONE
                }

                backButtonImageView?.visibility = View.GONE
                backButton?.removeCompoundDrawables()
                backButton?.setText(R.string.kenes_back)
                backButton?.setTextColor(ContextCompat.getColor(itemView.context, R.color.kenes_bright_blue))
                backButtonView?.isClickable = true
                backButtonView?.isFocusable = true
                backButtonView?.background = ResourcesCompat.getDrawable(
                    itemView.resources,
                    R.drawable.kenes_bg_rounded_horizonal_button,
                    itemView.context.theme
                )
                backButtonView?.setOnClickListener { callback?.onGoBackClicked(category) }
            }
        }
    }

    interface Callback {
        fun onShowAllCategoryChildClicked(category: Category)

        fun onCategoryChildClicked(category: Category)
        fun onGoBackClicked(category: Category)
        fun onUrlInTextClicked(url: String)

        fun onImageClicked(imageView: ImageView, imageUrl: String)
        fun onImageClicked(imageView: ImageView, bitmap: Bitmap)
        fun onImageLoadCompleted()

        fun onMediaClicked(media: Media, itemPosition: Int)
        fun onAttachmentClicked(attachment: Attachment, itemPosition: Int)

        fun onReplyMarkupButtonClicked(button: Message.ReplyMarkup.Button)

        fun onStopTrackingTouch(progress: Int, itemPosition: Int)
    }

}