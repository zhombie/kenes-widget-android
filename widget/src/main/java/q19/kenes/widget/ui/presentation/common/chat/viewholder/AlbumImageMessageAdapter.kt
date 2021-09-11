package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.textview.MaterialTextView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.domain.model.media.Media
import kz.q19.utils.view.inflate
import kz.zhombie.museum.ViewHolderDelegate
import q19.kenes.widget.ui.components.KenesSquareImageView
import q19.kenes_widget.R

internal class AlbumImageMessageAdapter constructor(
    var callback: Callback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = AlbumImageMessageAdapter::class.java.simpleName
    }

    object ViewType {
        const val IMAGE = 100
        const val LAST_IMAGE = 101
    }

    private var totalCount: Int = 0

    private var allImages = emptyList<Media>()

    private var images: List<Media> = emptyList()
        set(value) {
            field = value
            totalCount = field.size
            notifyDataSetChanged()
        }

    fun setData(images: List<Media>) {
        this.allImages = images
        this.images = images.take(4)
    }

    private fun getItem(position: Int): Media {
        return images[position]
    }

    private fun isLastItem(position: Int): Boolean {
        return images.isNotEmpty() && images.lastIndex == position
    }

    override fun getItemViewType(position: Int): Int {
        return if (totalCount > 4) {
            if (isLastItem(position)) {
                ViewType.LAST_IMAGE
            } else {
                ViewType.IMAGE
            }
        } else {
            ViewType.IMAGE
        }
    }

    override fun getItemCount(): Int = images.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.IMAGE ->
                ImageViewHolder(parent.inflate(R.layout.kenes_cell_chat_message_album_image))
            ViewType.LAST_IMAGE ->
                LastImageViewHolder(parent.inflate(R.layout.kenes_cell_chat_message_album_last_image))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder) {
            holder.bind(getItem(position))
        } else if (holder is LastImageViewHolder) {
            holder.bind(getItem(position), totalCount)
        }
    }

    private inner class ImageViewHolder constructor(
        view: View
    ) : RecyclerView.ViewHolder(view), ViewHolderDelegate {
        private val imageView = view.findViewById<KenesSquareImageView>(R.id.imageView)

        fun bind(image: Media) {
            imageView.load(image.urlPath)

            imageView.setOnClickListener {
                callback?.onImagesClicked(allImages, allImages.indexOf(image))
            }
        }

        override fun getImageView(): ImageView {
            return imageView
        }
    }

    private inner class LastImageViewHolder constructor(
        view: View
    ) : RecyclerView.ViewHolder(view), ViewHolderDelegate {
        private val imageView = view.findViewById<KenesSquareImageView>(R.id.imageView)
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(image: Media, totalCount: Int) {
            imageView.load(image.urlPath)

            textView.text = "Показать все ($totalCount)"

            itemView.setOnClickListener {
                callback?.onShowAllImagesClicked(allImages, allImages.lastIndex)
            }
        }

        override fun getImageView(): ImageView {
            return imageView
        }
    }

    interface Callback {
        fun onImagesClicked(images: List<Media>, imagePosition: Int)
        fun onShowAllImagesClicked(images: List<Media>, imagePosition: Int)
    }

}