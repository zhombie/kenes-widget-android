package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.components.KenesTextView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.common.SpacingItemDecoration
import q19.kenes_widget.R

internal abstract class BaseImageAlbumMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseMessageViewHolder(view), AlbumImageMessageAdapter.Callback {

    protected val albumView: RecyclerView? = view.findViewById(R.id.albumView)
    protected val textView: KenesTextView? = view.findViewById(R.id.textView)
    protected val timeView: KenesChatMessageTimeView? = view.findViewById(R.id.timeView)

    private var adapter: AlbumImageMessageAdapter? = null

    init {
        adapter = AlbumImageMessageAdapter(this)
        albumView?.layoutManager =
            GridLayoutManager(itemView.context, 2, GridLayoutManager.VERTICAL, false)
        albumView?.addItemDecoration(SpacingItemDecoration(1.5F.dp2Px()))
        albumView?.adapter = adapter
    }

    override fun bind(message: Message) {
        val attachments = message.attachments
        if (!attachments.isNullOrEmpty()) {
            adapter?.setData(attachments)
        } else {
            adapter?.setData(emptyList())
        }

        if (message.htmlText.isNullOrBlank()) {
            textView?.visibility = View.GONE
        } else {
            textView?.setHtmlText(message.htmlText) { _, url ->
                callback?.onUrlInTextClicked(url)
            }

            textView?.setOnLongClickListener {
                callback?.onMessageLongClicked(message.htmlText.toString())
                true
            }

            textView?.visibility = View.VISIBLE
        }

        timeView?.text = message.time
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