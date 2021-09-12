package kz.q19.kenes.widget.ui.presentation.common.chat

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kz.q19.utils.view.inflate
import kz.q19.kenes.widget.R

internal class ChatMessagesHeaderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ChatMessagesHeaderAdapter::class.java.simpleName
    }

    private var size: Int = 1

    override fun getItemCount(): Int = size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.kenes_cell_chat_messages_header))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind()
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind() {
        }

    }

}