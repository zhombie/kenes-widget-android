package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.domain.model.media.Media
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class IncomingImageAlbumMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseImageAlbumMessageViewHolder(view, callback), AlbumImageMessageAdapter.Callback {

    companion object {
        private val TAG = IncomingImageAlbumMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_image_album_message
    }

    init {
        setAlbumImageMessageCallback(this)
    }

    override fun onImagesClicked(images: List<Media>, imagePosition: Int) {
        if (albumView != null) {
            callback?.onImagesClicked(albumView, images, imagePosition)
        }
    }

    override fun onShowAllImagesClicked(images: List<Media>, imagePosition: Int) {
        if (albumView != null) {
            callback?.onImagesClicked(albumView, images, imagePosition)
        }
    }

}