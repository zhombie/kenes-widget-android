package q19.kenes_widget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.Message

internal class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_SELF_MESSAGE = R.layout.kenes_cell_my_message
        val LAYOUT_OPPONENT_MESSAGE = R.layout.kenes_cell_opponent_message
        val LAYOUT_NOTIFICATION = R.layout.kenes_cell_notification
        val LAYOUT_TYPING = R.layout.kenes_cell_typing
        val LAYOUT_CATEGORY = R.layout.kenes_cell_category
    }

    private var messages: MutableList<Message> = mutableListOf()

    fun addNewMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun setNewMessages(messages: List<Message>) {
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
            else ->
                LAYOUT_OPPONENT_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            LAYOUT_SELF_MESSAGE -> {
                val view = inflater.inflate(LAYOUT_SELF_MESSAGE, parent, false)
                SelfMessageViewHolder(view)
            }
            LAYOUT_NOTIFICATION -> {
                val view = inflater.inflate(LAYOUT_NOTIFICATION, parent, false)
                NotificationViewHolder(view)
            }
            LAYOUT_TYPING -> {
                val view = inflater.inflate(LAYOUT_TYPING, parent, false)
                TypingViewHolder(view)
            }
            LAYOUT_CATEGORY -> {
                val view = inflater.inflate(LAYOUT_CATEGORY, parent, false)
                CategoryViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(LAYOUT_OPPONENT_MESSAGE, parent, false)
                OpponentMessageViewHolder(view)
            }
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

    private inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var titleView: TextView? = null
        private var recyclerView: RecyclerView? = null
        private var adapter: SectionsAdapter

        init {
            titleView = view.findViewById(R.id.titleView)
            recyclerView = view.findViewById(R.id.recyclerView)

            recyclerView?.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = SectionsAdapter()
            recyclerView?.adapter = adapter
        }

        fun bind(message: Message) {
            val category = message.category
            if (category != null) {
                titleView?.text = category.title

                adapter.category = category
                adapter.notifyDataSetChanged()

//                Log.d("LOL", "BIND -> CATEGORY: " + message.category)
            }
        }
    }

}