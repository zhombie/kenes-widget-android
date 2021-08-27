package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.message.Message
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.components.KenesTextView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.common.chat.SpacingItemDecoration
import q19.kenes_widget.R

internal class IncomingImageAlbumMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : MessageViewHolder(view) {

    companion object {
        private val TAG = IncomingImageAlbumMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_incoming_image_album_message
    }

    private val albumView = view.findViewById<RecyclerView>(R.id.albumView)
    private val textView = view.findViewById<KenesTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    private var adapter: AlbumImageMessageAdapter? = null

    init {
        adapter = AlbumImageMessageAdapter()
        albumView.layoutManager =
            GridLayoutManager(itemView.context, 2, GridLayoutManager.VERTICAL, false)
        albumView.addItemDecoration(SpacingItemDecoration(1.5F.dp2Px()))
        albumView.adapter = adapter
    }

    override fun bind(message: Message) {
        adapter?.images = message.attachments?.take(4) ?: emptyList()

        if (message.htmlText.isNullOrBlank()) {
            textView.visibility = View.GONE
        } else {
            textView.setHtmlText(message.htmlText) { _, url ->
                callback?.onUrlInTextClicked(url)
            }

            textView.setOnLongClickListener {
                callback?.onMessageLongClicked(message.htmlText.toString())
                true
            }

            textView.visibility = View.VISIBLE
        }

        timeView.text = message.time
    }

}