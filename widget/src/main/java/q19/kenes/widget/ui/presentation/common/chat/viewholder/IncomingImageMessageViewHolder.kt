package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class IncomingImageMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseImageMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingImageMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_image_message
    }

}