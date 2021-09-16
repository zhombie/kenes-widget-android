package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.base.BaseVideoMessageViewHolder

internal class IncomingVideoMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseVideoMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingVideoMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_video_message
    }

}