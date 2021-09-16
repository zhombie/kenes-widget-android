package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.base.BaseImageMessageViewHolder

internal class IncomingImageMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseImageMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingImageMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_image_message
    }

}