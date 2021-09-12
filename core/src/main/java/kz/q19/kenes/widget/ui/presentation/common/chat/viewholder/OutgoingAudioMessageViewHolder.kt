package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.R

internal class OutgoingAudioMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseAudioMessageViewHolder(view, callback) {

    companion object {
        private val TAG = OutgoingAudioMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_outgoing_audio_message
    }

}