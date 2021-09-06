package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class OutgoingVideoMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseVideoMessageViewHolder(view, callback) {

    companion object {
        private val TAG = OutgoingVideoMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_video_message
    }

}