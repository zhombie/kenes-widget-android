package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.base

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.message.Message
import kz.q19.utils.android.dp2Px
import kz.q19.kenes.widget.ui.components.KenesChatMessageTimeView
import kz.q19.kenes.widget.ui.components.KenesTextView
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.ui.presentation.common.recycler_view.SpacingItemDecoration
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.AlbumImageMessageAdapter

internal abstract class BaseImageAlbumMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseMessageViewHolder(view) {

    protected val albumView: RecyclerView? = view.findViewById(R.id.albumView)
    protected val textView: KenesTextView? = view.findViewById(R.id.textView)
    protected val timeView: KenesChatMessageTimeView? = view.findViewById(R.id.timeView)

    private var adapter: AlbumImageMessageAdapter? = null

    init {
        adapter = AlbumImageMessageAdapter()
        albumView?.layoutManager =
            GridLayoutManager(itemView.context, 2, GridLayoutManager.VERTICAL, false)
        albumView?.addItemDecoration(SpacingItemDecoration(1.5F.dp2Px()))
        albumView?.adapter = adapter
    }

    fun setAlbumImageMessageCallback(callback: AlbumImageMessageAdapter.Callback?) {
        adapter?.callback = callback
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

}