package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.base.BaseTextMessageViewHolder

internal class OutgoingTextMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseTextMessageViewHolder(view, callback) {

    companion object {
        private val TAG = OutgoingTextMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_outgoing_text_message
    }

}