package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kz.q19.domain.model.media.Media
import kz.q19.utils.view.inflate
import q19.kenes.widget.util.loadImage
import q19.kenes_widget.R

internal class AlbumImageMessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = AlbumImageMessageAdapter::class.java.simpleName
    }

    var images: List<Media> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): Media {
        return images[position]
    }

    private fun isLastItem(position: Int): Boolean {
        return images.isNotEmpty() && images.lastIndex == position
    }

    override fun getItemCount(): Int = images.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.cell_chat_message_album_image))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)

        fun bind(image: Media) {
            imageView.loadImage(image.urlPath)

            if (isLastItem(bindingAdapterPosition)) {

            } else {

            }

            imageView.setOnClickListener {  }
        }
    }

}