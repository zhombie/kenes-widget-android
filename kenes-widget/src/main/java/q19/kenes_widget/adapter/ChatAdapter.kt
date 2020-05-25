package q19.kenes_widget.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import q19.kenes_widget.R
import q19.kenes_widget.model.Category
import q19.kenes_widget.model.Media
import q19.kenes_widget.model.Message
import q19.kenes_widget.util.HtmlTextViewManager
import q19.kenes_widget.util.picasso.RoundedTransformation

internal class ChatAdapter(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_USER_MESSAGE = R.layout.kenes_cell_user_message
        val LAYOUT_OPPONENT_MESSAGE = R.layout.kenes_cell_opponent_message
        val LAYOUT_NOTIFICATION = R.layout.kenes_cell_notification
        val LAYOUT_TYPING = R.layout.kenes_cell_typing
        val LAYOUT_CATEGORY = R.layout.kenes_cell_category
        val LAYOUT_CROSS_CHILDREN = R.layout.kenes_cell_cross_children
        val LAYOUT_RESPONSE = R.layout.kenes_cell_response
    }

    private var messages: MutableList<Message> = mutableListOf()

    var isGoToHomeButtonEnabled: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun addNewMessage(message: Message, isNotifyEnabled: Boolean = true) {
        messages.add(message)
        if (isNotifyEnabled) {
            notifyItemInserted(messages.size - 1)
        }
    }

    fun addNewMessages(messages: List<Message>, isNotifyEnabled: Boolean = true) {
        if (messages.isEmpty()) return
        val last = this.messages.size - 1
        this.messages.addAll(last, messages)
        if (isNotifyEnabled) {
            notifyItemRangeInserted(last, messages.size)
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

    fun removeLastMessage(isNotifyEnabled: Boolean) {
        messages.dropLast(1)
        if (isNotifyEnabled) {
            notifyItemRemoved(messages.size - 1)
        }
    }

    fun clearMessages(isNotifyEnabled: Boolean = true) {
        messages.clear()
        if (isNotifyEnabled) {
            notifyDataSetChanged()
        }
    }

    fun clearCategoryMessages(): Boolean {
        return messages.removeAll { it.category != null }
    }

    fun hasMessages(): Boolean {
        return !messages.isNullOrEmpty()
    }

    fun isAllMessagesAreCategory(): Boolean {
        return messages.all {
            it.type == Message.Type.CATEGORY || it.type == Message.Type.CROSS_CHILDREN || it.type == Message.Type.RESPONSE
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            Message.Type.USER ->
                LAYOUT_USER_MESSAGE
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
            Message.Type.OPPONENT ->
                LAYOUT_OPPONENT_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(viewType, parent, false)

        return when (viewType) {
            LAYOUT_USER_MESSAGE -> UserMessageViewHolder(view)
            LAYOUT_NOTIFICATION -> NotificationViewHolder(view)
            LAYOUT_TYPING -> TypingViewHolder(view)
            LAYOUT_CATEGORY -> CategoryViewHolder(view)
            LAYOUT_CROSS_CHILDREN -> CrossChildrenViewHolder(view)
            LAYOUT_RESPONSE -> ResponseViewHolder(view)
            LAYOUT_OPPONENT_MESSAGE -> OpponentMessageViewHolder(view)
            else -> throw IllegalStateException("There is no ViewHolder for viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (message.type) {
            Message.Type.USER ->
                if (holder is UserMessageViewHolder) holder.bind(message)
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
            Message.Type.OPPONENT ->
                if (holder is OpponentMessageViewHolder) holder.bind(message)
        }
    }

    private inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView: TextView? = null
        private var timeView: TextView? = null

        init {
            textView = view.findViewById(R.id.textView)
            timeView = view.findViewById(R.id.timeView)
        }

        fun bind(message: Message) {
            textView?.text = message.text
            timeView?.text = message.time
        }
    }

    private inner class OpponentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var messageView: LinearLayout? = null
        private var imageView: ImageView? = null
        private var fileView: TextView? = null
        private var textView: TextView? = null
        private var timeView: TextView? = null
        private var goToHomeButton: AppCompatButton? = null

        private var htmlTextViewManager = HtmlTextViewManager()

        init {
            messageView = view.findViewById(R.id.messageView)
            imageView = view.findViewById(R.id.imageView)
            fileView = view.findViewById(R.id.fileView)
            textView = view.findViewById(R.id.textView)
            timeView = view.findViewById(R.id.timeView)
            goToHomeButton = view.findViewById(R.id.goToHomeButton)

            goToHomeButton?.setOnClickListener { callback.onGoToHomeClicked() }
        }

//        val target = object : Target() {
//            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
//                super.onPrepareLoad(placeHolderDrawable)
//                imageView?.visibility = View.VISIBLE
//            }
//
//            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
//                bitmap?.let {
//                    val ratio = bitmap.height / bitmap.width
//                    imageView?.heightRatio = ratio.toFloat()
//
//                    imageView?.setImageBitmap(bitmap)
//                }
//            }
//        }

        fun bind(message: Message) {
            message.media?.let { media ->
                if (media.isImage) {
                    imageView?.visibility = View.VISIBLE

                    Picasso.get()
                        .load(media.imageUrl)
                        .placeholder(R.drawable.kenes_bg_gradient_gray)
                        .transform(RoundedTransformation(
                            itemView.resources.getDimensionPixelOffset(R.dimen.kenes_message_background_corner_radius)
                        ))
                        .priority(Picasso.Priority.HIGH)
                        .into(imageView)

                    itemView.setOnClickListener {
                        callback.onImageClicked(
                            imageView ?: return@setOnClickListener,
                            media.imageUrl ?: return@setOnClickListener
                        )
                    }
                } else {
                    imageView?.visibility = View.GONE
                }

                if (media.isFile) {
                    if (media.fileTypeStringRes != null) {
                        fileView?.text = media.name + "\n(" + itemView.context.getString(media.fileTypeStringRes!!) + ")"
                    } else {
                        fileView?.text = media.name
                    }

                    fileView?.setOnClickListener {
                        callback.onFileClicked(media)
                    }

                    fileView?.visibility = View.VISIBLE
                } else {
                    fileView?.visibility = View.GONE
                }
            }

            if (message.text.isNotBlank()) {
                htmlTextViewManager.setHtmlText(textView, message.htmlText)
                htmlTextViewManager.setOnUrlClickListener { _, url ->
                    callback.onUrlInTextClicked(url)
                }
                textView?.visibility = View.VISIBLE
            } else {
                textView?.visibility = View.GONE
            }

            timeView?.text = message.time

            goToHomeButton?.visibility =
                if (adapterPosition == itemCount - 1 && isGoToHomeButtonEnabled) View.VISIBLE
                else View.GONE
        }
    }

    private inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView: TextView? = null
        private var timeView: TextView? = null

        init {
            textView = view.findViewById(R.id.textView)
            timeView = view.findViewById(R.id.timeView)
        }

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
        private var titleView: TextView? = null
        private var recyclerView: RecyclerView? = null

        private var categoryAdapter: CategoryAdapter
        private var layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)

        init {
            titleView = view.findViewById(R.id.titleView)
            recyclerView = view.findViewById(R.id.recyclerView)

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
            }
        }

        override fun onChildClicked(category: Category) {
            callback.onCategoryChildClicked(category)
        }

        private inner class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

            private var horizontalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.kenes_message_horizontal_spacing)

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
        private var titleView: TextView? = null
        private var recyclerView: RecyclerView? = null
        private var goToHomeButton: AppCompatButton? = null

        private var crossChildrenAdapter: CrossChildrenAdapter
        private var layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

        init {
            titleView = view.findViewById(R.id.titleView)
            recyclerView = view.findViewById(R.id.recyclerView)
            goToHomeButton = view.findViewById(R.id.goToHomeButton)

            recyclerView?.layoutManager = layoutManager
            crossChildrenAdapter = CrossChildrenAdapter(this)
            recyclerView?.adapter = crossChildrenAdapter
            recyclerView?.addItemDecoration(ItemDecoration(itemView.context))

            goToHomeButton?.visibility = View.VISIBLE
            goToHomeButton?.setOnClickListener { callback.onGoToHomeClicked() }
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                crossChildrenAdapter.category = category
                crossChildrenAdapter.notifyDataSetChanged()

                titleView?.setOnClickListener {
                    callback.onReturnBackClicked(category)
                }
            }
        }

        override fun onCrossChildClicked(category: Category) {
            callback.onCategoryChildClicked(category)
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
        private var titleView: TextView? = null
        private var textView: TextView? = null
        private var timeView: TextView? = null
        private var goToHomeButton: AppCompatButton? = null

        private var htmlTextViewManager = HtmlTextViewManager()

        init {
            titleView = view.findViewById(R.id.titleView)
            textView = view.findViewById(R.id.textView)
            timeView = view.findViewById(R.id.timeView)
            goToHomeButton = view.findViewById(R.id.goToHomeButton)

            goToHomeButton?.visibility = View.VISIBLE
            goToHomeButton?.setOnClickListener { callback.onGoToHomeClicked() }

//            textView?.movementMethod = LinkMovementMethod.getInstance()
        }

        fun bind(message: Message) {
            val category = message.category

            if (category != null) {
                titleView?.text = message.category?.title

                htmlTextViewManager.setHtmlText(textView, message.htmlText)
                htmlTextViewManager.setOnUrlClickListener { _, url ->
                    callback.onUrlInTextClicked(url)
                }
//                textView?.setText(message.htmlText, TextView.BufferType.SPANNABLE)

                timeView?.text = message.time

                titleView?.setOnClickListener {
                    callback.onReturnBackClicked(category)
                }
            }
        }
    }

    interface Callback {
        fun onCategoryChildClicked(category: Category)
        fun onGoToHomeClicked()
        fun onReturnBackClicked(category: Category)
        fun onUrlInTextClicked(url: String)

        fun onImageClicked(imageView: ImageView, imageUrl: String)
        fun onImageClicked(imageView: ImageView, bitmap: Bitmap)
        fun onImageLoadCompleted()

        fun onFileClicked(media: Media)
    }

}