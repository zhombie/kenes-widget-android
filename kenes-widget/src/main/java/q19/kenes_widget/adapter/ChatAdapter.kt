package q19.kenes_widget.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.model.Category
import q19.kenes_widget.model.Media
import q19.kenes_widget.model.Message
import q19.kenes_widget.util.ColorStateListBuilder
import q19.kenes_widget.util.HtmlTextViewManager
import q19.kenes_widget.util.inflate
import q19.kenes_widget.util.picasso.RoundedTransformation

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

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages.isNotEmpty()) {
            when (messages[position].type) {
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
        val message = messages[position]

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

    private inner class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var imageView = view.findViewById<ImageView>(R.id.imageView)
        private var fileView = view.findViewById<TextView>(R.id.fileView)
        private var textView = view.findViewById<TextView>(R.id.textView)
        private var timeView = view.findViewById<TextView>(R.id.timeView)

        init {
            timeView.visibility = View.GONE
        }

        fun bind(message: Message) {
            val media = message.media
            if (media == null) {
                imageView?.visibility = View.GONE
                fileView?.visibility = View.GONE
            } else {
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
                    if (media.fileTypeStringRes != null) {
                        fileView?.text = media.name + "\n(" + itemView.context.getString(media.fileTypeStringRes!!) + ")"
                    } else {
                        fileView?.text = media.name
                    }

                    fileView?.setOnClickListener {
                        callback?.onFileClicked(media)
                    }

                    fileView?.visibility = View.VISIBLE

                    timeView?.text = message.time
                    timeView?.visibility = View.VISIBLE
                } else {
                    fileView?.visibility = View.GONE
                }
            }

            if (message.text.isNotBlank()) {
                textView?.text = message.text
                timeView?.text = message.time

                textView?.visibility = View.VISIBLE
                timeView?.visibility = View.VISIBLE
            } else {
                textView?.visibility = View.GONE
            }
        }
    }

    private inner class OpponentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var imageView = view.findViewById<ImageView>(R.id.imageView)
        private var fileView = view.findViewById<TextView>(R.id.fileView)
        private var textView = view.findViewById<TextView>(R.id.textView)
        private var timeView = view.findViewById<TextView>(R.id.timeView)

        private var htmlTextViewManager = HtmlTextViewManager()

        init {
            timeView.visibility = View.GONE
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
            val media = message.media
            if (media == null) {
                imageView?.visibility = View.GONE
                fileView?.visibility = View.GONE
            } else {
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
                    if (media.fileTypeStringRes != null) {
                        fileView?.text = media.name + "\n(" + itemView.context.getString(media.fileTypeStringRes!!) + ")"
                    } else {
                        fileView?.text = media.name
                    }

                    fileView?.setOnClickListener {
                        callback?.onFileClicked(media)
                    }

                    fileView?.visibility = View.VISIBLE

                    timeView?.text = message.time
                    timeView?.visibility = View.VISIBLE
                } else {
                    fileView?.visibility = View.GONE
                }
            }

            if (message.text.isNotBlank()) {
                htmlTextViewManager.setHtmlText(textView, message.htmlText)
                htmlTextViewManager.setOnUrlClickListener { _, url ->
                    callback?.onUrlInTextClicked(url)
                }

                val colorStateList = ColorStateListBuilder()
                    .addState(IntArray(1) { android.R.attr.state_pressed }, ContextCompat.getColor(itemView.context, R.color.kenes_blue))
                    .addState(IntArray(1) { android.R.attr.state_selected }, ContextCompat.getColor(itemView.context, R.color.kenes_blue))
                    .addState(intArrayOf(), ContextCompat.getColor(itemView.context, R.color.kenes_blue))
                    .build()

                textView.highlightColor = Color.TRANSPARENT

                textView.setLinkTextColor(colorStateList)

                timeView?.text = message.time

                textView?.visibility = View.VISIBLE
                timeView?.visibility = View.VISIBLE
            } else {
                textView?.visibility = View.GONE
            }
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

        private var htmlTextViewManager = HtmlTextViewManager()

        fun bind(message: Message) {
            val category = message.category

            if (category != null) {
                titleView.text = message.category?.title

                if (message.text.isNotBlank()) {
                    htmlTextViewManager.setHtmlText(textView, message.htmlText)
                    htmlTextViewManager.setOnUrlClickListener { _, url ->
                        callback?.onUrlInTextClicked(url)
                    }

                    val colorStateList = ColorStateListBuilder()
                        .addState(IntArray(1) { android.R.attr.state_pressed }, ContextCompat.getColor(itemView.context, R.color.kenes_blue))
                        .addState(IntArray(1) { android.R.attr.state_selected }, ContextCompat.getColor(itemView.context, R.color.kenes_blue))
                        .addState(intArrayOf(), ContextCompat.getColor(itemView.context, R.color.kenes_blue))
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

//                textView?.setText(message.htmlText, TextView.BufferType.SPANNABLE)
//                textView?.movementMethod = LinkMovementMethod.getInstance()

                titleView.setOnClickListener {
                    callback?.onReturnBackClicked(category)
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

        fun onFileClicked(media: Media)
    }

}