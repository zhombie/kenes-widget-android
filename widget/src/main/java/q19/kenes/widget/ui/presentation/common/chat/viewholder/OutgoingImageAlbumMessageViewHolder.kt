package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class OutgoingImageAlbumMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseImageAlbumMessageViewHolder(view, callback) {

    companion object {
        private val TAG = OutgoingImageAlbumMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_image_album_message
    }

}