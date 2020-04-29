package q19.kenes_widget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.models.Message

internal class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_MY_MESSAGE = R.layout.cell_my_message
        private val LAYOUT_OPPONENT_MESSAGE = R.layout.cell_opponent_message
    }

    private var data: MutableList<Message> = mutableListOf()

    fun addNewItem(message: Message) {
        data.add(message)
        notifyItemInserted(data.size - 1)
    }

    fun clearItems() {
        data.clear()
        notifyItemRangeRemoved(0, data.size - 1)
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int {
        return if (data[position].from_me) {
            LAYOUT_MY_MESSAGE
        } else {
            LAYOUT_OPPONENT_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == LAYOUT_MY_MESSAGE) {
            val view = inflater.inflate(LAYOUT_MY_MESSAGE, parent, false)
            MyMessageViewHolder(view)
        } else {
            val view = inflater.inflate(LAYOUT_OPPONENT_MESSAGE, parent, false)
            OpponentMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = data[position]

        if (message.from_me) {
            if (holder is MyMessageViewHolder) {
                holder.bind(message)
            }
        } else {
            if (holder is OpponentMessageViewHolder) {
                holder.bind(message)
            }
        }
    }

    private inner class MyMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

}