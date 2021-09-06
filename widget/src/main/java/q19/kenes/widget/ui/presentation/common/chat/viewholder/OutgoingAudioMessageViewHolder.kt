package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class OutgoingAudioMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseAudioMessageViewHolder(view, callback) {

    companion object {
        private val TAG = OutgoingAudioMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_audio_message
    }

}