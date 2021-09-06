package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class IncomingTextMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseTextMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingTextMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_incoming_text_message
    }

}