package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class IncomingImageAlbumMessageViewHolder constructor(
    view: View,
    callback: ChatMessagesAdapter.Callback? = null
) : BaseImageAlbumMessageViewHolder(view, callback) {

    companion object {
        private val TAG = IncomingImageAlbumMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_incoming_image_album_message
    }

}