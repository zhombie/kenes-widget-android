package q19.kenes_widget.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.model.Attachment
import q19.kenes_widget.model.Category
import q19.kenes_widget.model.Media
import q19.kenes_widget.model.Message
import q19.kenes_widget.util.*

internal class ChatAdapter(
    var callback: Callback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_USER_MESSAGE = R.layout.kenes_cell_user_message
        private val LAYOUT_OPPONENT_MESSAGE = R.layout.kenes_cell_opponent_message
        val LAYOUT_NOTIFICATION = R.layout.kenes_cell_notification
        private val LAYOUT_TYPING = R.layout.kenes_cell_typing
        private val LAYOUT_CATEGORY = R.layout.kenes_cell_category
        private val LAYOUT_CROSS_CHILDREN = R.layout.kenes_cell_cross_children
        private val LAYOUT_RESPONSE = R.layout.kenes_cell_response

        private const val KEY_PROGRESS = "progress"
        private const val KEY_FILE_TYPE = "fileType"
    }

    private var messages = mutableListOf<Message>()

    fun addNewMessage(message: Message, isNotifyEnabled: Boolean = true) {
        messages.add(message)

        if (isNotifyEnabled) {
            notifyItemInserted(messages.size - 1)
        }
    }

    fun setNewMessages(messages: List<Message>, isNotifyEnabled: Boolean = true) {
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

    fun setDownloading(position: Int, downloadStatus: Message.File.DownloadStatus) {
        getItem(position).file.downloadStatus = downloadStatus
        notifyItemChanged(position)
    }

    fun setProgress(position: Int, fileType: String, progress: Int) {
        getItem(position).apply {
            file.progress = progress
            file.type = fileType
        }
        notifyItemChanged(position, Bundle().apply {
            putInt(KEY_PROGRESS, progress)
            putString(KEY_FILE_TYPE, fileType)
        })
    }

    private fun getItem(position: Int) = messages[position]

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages.isNotEmpty()) {
            when (getItem(position).type) {
                Message.Type.USER ->
                    LAYOUT_USER_MESSAGE
                Message.Type.OPPONENT ->
                    LAYOUT_OPPONENT_MESSAGE
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
            LAYOUT_USER_MESSAGE -> UserMessageViewHolder(view)
            LAYOUT_OPPONENT_MESSAGE -> OpponentMessageViewHolder(view)
            LAYOUT_NOTIFICATION -> NotificationViewHolder(view)
            LAYOUT_TYPING -> TypingViewHolder(view)
            LAYOUT_CATEGORY -> CategoryViewHolder(view)
            LAYOUT_CROSS_CHILDREN -> CrossChildrenViewHolder(view)
            LAYOUT_RESPONSE -> ResponseViewHolder(view)
            else -> throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (message.type) {
            Message.Type.USER ->
                if (holder is UserMessageViewHolder) holder.bind(message)
            Message.Type.OPPONENT ->
                if (holder is OpponentMessageViewHolder) holder.bind(message)
            Message.Type.NOTIFICATION ->
                if (holder is NotificationViewHolder) holder.bind(message)
            Message.Type.TYPING ->
                if (holder is TypingViewHolder) holder.bind()
            Message.Type.CATEGORY ->
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
        super.onBindViewHolder(holder, position, payloads)

        val payload = payloads.lastOrNull()
        if (payload != null) {
            if (holder is OpponentMessageViewHolder && payload is Bundle) {
                val message = getItem(position)
                val fileType = payload.getString(KEY_FILE_TYPE)

                if (fileType == "media") {
                    holder.updateMediaProgress(
                        message.media,
                        message.file.downloadStatus,
                        payload.getInt(KEY_PROGRESS)
                    )
                }
            }
        }
    }

    private inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var imageView = view.findViewById<ImageView>(R.id.imageView)
        private var mediaView = view.findViewById<LinearLayout>(R.id.mediaView)
        private var iconView = view.findViewById<ImageView>(R.id.iconView)
        private var mediaNameView = view.findViewById<TextView>(R.id.mediaNameView)
        private var textView = view.findViewById<TextView>(R.id.textView)
        private var timeView = view.findViewById<TextView>(R.id.timeView)

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
                            imageView ?: return@setOnClickListener,
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

                textView?.autoLinkMask = Linkify.ALL
                textView?.movementMethod = LinkMovementMethod.getInstance()

                val colorStateList = ColorStateListBuilder()
                    .addState(IntArray(1) { android.R.attr.state_pressed }, ContextCompat.getColor(context, R.color.kenes_blue))
                    .addState(IntArray(1) { android.R.attr.state_selected }, ContextCompat.getColor(context, R.color.kenes_blue))
                    .addState(intArrayOf(), ContextCompat.getColor(context, R.color.kenes_blue))
                    .build()

                textView?.highlightColor = Color.TRANSPARENT

                textView?.setLinkTextColor(colorStateList)

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
            spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.kenes_blue)),0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            spannableStringBuilder.append(spannableString)

            mediaNameView?.text = spannableStringBuilder

            return mediaNameView.text.isBlank()
        }
    }

    private inner class OpponentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var imageView = view.findViewById<ImageView>(R.id.imageView)
        private var mediaView = view.findViewById<LinearLayout>(R.id.mediaView)
        private var iconView = view.findViewById<ImageView>(R.id.iconView)
        private var progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        private var mediaNameView = view.findViewById<TextView>(R.id.mediaNameView)
        private var textView = view.findViewById<TextView>(R.id.textView)
        private var timeView = view.findViewById<TextView>(R.id.timeView)
        private var attachmentView = view.findViewById<TextView>(R.id.attachmentView)

        private var htmlTextViewManager = HtmlTextViewManager()

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
                            imageView, media.imageUrl ?: return@setOnClickListener
                        )
                    }

                    timeView?.text = message.time
                    timeView?.visibility = View.VISIBLE
                } else {
                    imageView?.visibility = View.GONE
                }

                if (media.isFile) {
                    val isEmptyMediaName = context.bindMedia(media, message.file.downloadStatus)

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
                htmlTextViewManager.setHtmlText(textView, message.htmlText)
                htmlTextViewManager.setOnUrlClickListener { _, url ->
                    callback?.onUrlInTextClicked(url)
                }

                val colorStateList = ColorStateListBuilder()
                    .addState(IntArray(1) { android.R.attr.state_pressed }, ContextCompat.getColor(context, R.color.kenes_blue))
                    .addState(IntArray(1) { android.R.attr.state_selected }, ContextCompat.getColor(context, R.color.kenes_blue))
                    .addState(intArrayOf(), ContextCompat.getColor(context, R.color.kenes_blue))
                    .build()

                textView?.highlightColor = Color.TRANSPARENT

                textView?.setLinkTextColor(colorStateList)

                timeView?.text = message.time

                textView?.visibility = View.VISIBLE
                timeView?.visibility = View.VISIBLE
            } else {
                textView?.visibility = View.GONE
            }

            val attachments = message.attachments
            if (!attachments.isNullOrEmpty()) {
                val attachment = attachments[0]

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

        fun updateMediaProgress(media: Media?, downloadStatus: Message.File.DownloadStatus, progress: Int) {
            progressBar.progress = if (progress == 0 || progress == 100) 0 else progress

            if (downloadStatus == Message.File.DownloadStatus.COMPLETED) {
                media?.let { itemView.context.bindMedia(it, downloadStatus) }
            }
        }

        private fun Context.bindMedia(media: Media, downloadStatus: Message.File.DownloadStatus): Boolean {
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
                progressBar.progress = 0

                iconView?.setImageResource(R.drawable.kenes_ic_document_white)
                val spannableString = SpannableString(getString(R.string.kenes_open_file))
                spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.kenes_blue)),0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                spannableStringBuilder.append(spannableString)
            } else {
                iconView?.setImageResource(R.drawable.kenes_ic_download_white)
                val spannableString = SpannableString(getString(R.string.kenes_file_download))
                spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.kenes_blue)),0, spannableString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                spannableStringBuilder.append(spannableString)
            }

            mediaNameView?.text = spannableStringBuilder

            return mediaNameView.text.isBlank()
        }
    }

    private inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView = view.findViewById<TextView>(R.id.textView)
        private var timeView = view.findViewById<TextView>(R.id.timeView)

        fun bind(message: Message) {
            textView?.text = message.text
            timeView?.text = message.time
        }
    }

    private inner class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
        }
    }

    private inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view), CategoryAdapter.Callback {
        private var titleView = view.findViewById<TextView>(R.id.titleView)
        private var recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        private var showAllButton = view.findViewById<AppCompatTextView>(R.id.showAllButton)

        private var categoryAdapter: CategoryAdapter
        private var layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)

        init {
            recyclerView?.layoutManager = layoutManager
            categoryAdapter = CategoryAdapter(this)
            recyclerView?.adapter = categoryAdapter
            recyclerView?.addItemDecoration(ItemDecoration(itemView.context))
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                categoryAdapter.category = category
                categoryAdapter.notifyDataSetChanged()

                showAllButton?.setOnClickListener {
                    callback?.showAllCategoryChildrenClicked(category)
                }
            }
        }

        override fun onChildClicked(category: Category) {
            callback?.onCategoryChildClicked(category)
        }

        private inner class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

            private var horizontalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.kenes_category_horizontal_spacing)

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

    private inner class CrossChildrenViewHolder(view: View) : RecyclerView.ViewHolder(view), CrossChildrenAdapter.Callback {
        private var titleView = view.findViewById<TextView>(R.id.titleView)
        private var recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private var crossChildrenAdapter: CrossChildrenAdapter
        private var layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

        init {
            recyclerView?.layoutManager = layoutManager
            crossChildrenAdapter = CrossChildrenAdapter(this)
            recyclerView?.adapter = crossChildrenAdapter
            recyclerView?.addItemDecoration(ItemDecoration(itemView.context))
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                crossChildrenAdapter.category = category
                crossChildrenAdapter.notifyDataSetChanged()

                titleView?.setOnClickListener {
                    callback?.onReturnBackClicked(category)
                }
            }
        }

        override fun onCrossChildClicked(category: Category) {
            callback?.onCategoryChildClicked(category)
        }

        private inner class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

            private var verticalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.kenes_message_vertical_spacing)

            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)

                outRect.bottom = verticalSpacing
            }
        }
    }

    private inner class ResponseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var titleView = view.findViewById<TextView>(R.id.titleView)
        private var textView = view.findViewById<TextView>(R.id.textView)
        private var timeView = view.findViewById<TextView>(R.id.timeView)
        private var attachmentView = view.findViewById<TextView>(R.id.attachmentView)

        private var htmlTextViewManager = HtmlTextViewManager()

        fun bind(message: Message) {
            val context = itemView.context

            val category = message.category

            if (category != null) {
                if (category.title.isNotBlank())  {
                    titleView.text = category.title
                    titleView.visibility = View.VISIBLE

                    titleView.setOnClickListener {
                        callback?.onReturnBackClicked(category)
                    }
                } else {
                    titleView.visibility = View.GONE
                }

                if (message.text.isNotBlank()) {
                    htmlTextViewManager.setHtmlText(textView, message.htmlText)
                    htmlTextViewManager.setOnUrlClickListener { _, url ->
                        callback?.onUrlInTextClicked(url)
                    }

                    val colorStateList = ColorStateListBuilder()
                        .addState(IntArray(1) { android.R.attr.state_pressed }, ContextCompat.getColor(context, R.color.kenes_blue))
                        .addState(IntArray(1) { android.R.attr.state_selected }, ContextCompat.getColor(context, R.color.kenes_blue))
                        .addState(intArrayOf(), ContextCompat.getColor(context, R.color.kenes_blue))
                        .build()

                    textView.highlightColor = Color.TRANSPARENT

                    textView.setLinkTextColor(colorStateList)

                    timeView.text = message.time

                    textView.visibility = View.VISIBLE
                    timeView.visibility = View.VISIBLE
                } else {
                    textView.visibility = View.GONE
                    timeView.visibility = View.GONE
                }

                val attachments = message.attachments
                if (!attachments.isNullOrEmpty()) {
                    val attachment = attachments[0]

                    attachmentView.text = attachment.title
                    attachmentView.visibility = View.VISIBLE

                    attachmentView.setOnClickListener {
                        callback?.onAttachmentClicked(attachment, absoluteAdapterPosition)
                    }
                } else {
                    attachmentView.visibility = View.GONE
                }
            }
        }
    }

    interface Callback {
        fun showAllCategoryChildrenClicked(category: Category)

        fun onCategoryChildClicked(category: Category)
        fun onReturnBackClicked(category: Category)
        fun onUrlInTextClicked(url: String)

        fun onImageClicked(imageView: ImageView, imageUrl: String)
        fun onImageClicked(imageView: ImageView, bitmap: Bitmap)
        fun onImageLoadCompleted()

        fun onMediaClicked(media: Media, position: Int)
        fun onAttachmentClicked(attachment: Attachment, position: Int)
    }

}