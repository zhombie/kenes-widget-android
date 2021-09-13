package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import android.widget.ImageView
import kz.q19.domain.model.media.Media
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter

internal class OutgoingImageAlbumMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseImageAlbumMessageViewHolder(view, callback), AlbumImageMessageAdapter.Callback {

    companion object {
        private val TAG = OutgoingImageAlbumMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_outgoing_image_album_message
    }

    init {
        setAlbumImageMessageCallback(this)
    }

    override fun onImageClicked(imageView: ImageView, image: Media) {
        callback?.onImageClicked(imageView, image)
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