package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import android.widget.ImageView
import kz.q19.domain.model.media.Media
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.base.BaseImageAlbumMessageViewHolder

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