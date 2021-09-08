package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class IncomingAudioMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseAudioMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingAudioMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_audio_message
    }

}