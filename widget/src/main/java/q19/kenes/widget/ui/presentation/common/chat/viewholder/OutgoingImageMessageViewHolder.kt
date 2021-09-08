package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class OutgoingImageMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseImageMessageViewHolder(view, callback) {

    companion object {
        private val TAG = OutgoingImageMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_outgoing_image_message
    }

}