package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.R

internal class IncomingTextMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseTextMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingTextMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_text_message
    }

}