package q19.kenes_widget.adapter

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.Message
import q19.kenes_widget.model.Category

internal class ChatAdapter(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_SELF_MESSAGE = R.layout.kenes_cell_my_message
        val LAYOUT_OPPONENT_MESSAGE = R.layout.kenes_cell_opponent_message
        val LAYOUT_NOTIFICATION = R.layout.kenes_cell_notification
        val LAYOUT_TYPING = R.layout.kenes_cell_typing
        val LAYOUT_CATEGORY = R.layout.kenes_cell_category
        val LAYOUT_CROSS_CHILDREN = R.layout.kenes_cell_cross_children
    }

    private var messages: MutableList<Message> = mutableListOf()

    fun addNewMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun addNewMessages(messages: List<Message>) {
        val last = this.messages.size - 1
        this.messages.addAll(last, messages)
        notifyItemRangeInserted(last, messages.size)
    }

    fun setNewMessages(messages: List<Message>) {
        Log.d("LOL", "setNewMessages(messages: $messages)")
        if (messages.isNotEmpty()) {
            this.messages.clear()
        }
        this.messages.addAll(messages)
        notifyDataSetChanged()
//        notifyItemRangeInserted(0, messages.size - 1)
    }

    fun removeLastMessage() {
        messages.dropLast(1)
        notifyItemRemoved(messages.size - 1)
    }

    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
//        notifyItemRangeRemoved(0, messages.size - 1)
    }

    fun hasMessages(): Boolean {
        return !messages.isNullOrEmpty()
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            Message.Type.SELF ->
                LAYOUT_SELF_MESSAGE
            Message.Type.NOTIFICATION ->
                LAYOUT_NOTIFICATION
            Message.Type.TYPING ->
                LAYOUT_TYPING
            Message.Type.CATEGORY ->
                LAYOUT_CATEGORY
            Message.Type.CROSS_CHILDREN ->
                LAYOUT_CROSS_CHILDREN
            else ->
                LAYOUT_OPPONENT_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(viewType, parent, false)

        return when (viewType) {
            LAYOUT_SELF_MESSAGE -> SelfMessageViewHolder(view)
            LAYOUT_NOTIFICATION -> NotificationViewHolder(view)
            LAYOUT_TYPING -> TypingViewHolder(view)
            LAYOUT_CATEGORY -> CategoryViewHolder(view)
            LAYOUT_CROSS_CHILDREN -> CrossChildrenViewHolder(view)
            else -> OpponentMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (message.type == Message.Type.SELF) {
            if (holder is SelfMessageViewHolder) {
                holder.bind(message)
            }
        } else if (message.type == Message.Type.NOTIFICATION) {
            if (holder is NotificationViewHolder) {
                holder.bind(message)
            }
        } else if (message.type == Message.Type.TYPING) {
            if (holder is TypingViewHolder) {
                holder.bind()
            }
        } else if (message.type == Message.Type.CATEGORY) {
            if (holder is CategoryViewHolder) {
                holder.bind(message)
            }
        } else if (message.type == Message.Type.CROSS_CHILDREN) {
            if (holder is CrossChildrenViewHolder) {
                holder.bind(message)
            }
        } else if (message.type == Message.Type.OPPONENT) {
            if (holder is OpponentMessageViewHolder) {
                holder.bind(message)
            }
        }
    }

    private inner class SelfMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        init {
            titleView = view.findViewById(R.id.titleView)
            recyclerView = view.findViewById(R.id.recyclerView)

            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
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

        init {
            titleView = view.findViewById(R.id.titleView)
            recyclerView = view.findViewById(R.id.recyclerView)
            goToHomeButton = view.findViewById(R.id.goToHomeButton)

            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            crossChildrenAdapter = CrossChildrenAdapter(this)
            recyclerView?.adapter = crossChildrenAdapter
            recyclerView?.addItemDecoration(ItemDecoration(itemView.context))

            goToHomeButton?.setOnClickListener {
                callback.onGoToHomeClicked()
            }
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                crossChildrenAdapter.category = category
                crossChildrenAdapter.notifyDataSetChanged()
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

    interface Callback {
        fun onCategoryChildClicked(category: Category)
        fun onGoToHomeClicked()
    }

}