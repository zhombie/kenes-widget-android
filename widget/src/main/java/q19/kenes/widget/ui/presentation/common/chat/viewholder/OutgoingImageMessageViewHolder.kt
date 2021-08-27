package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import com.google.android.material.imageview.ShapeableImageView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTextView
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.util.loadImage
import q19.kenes_widget.R

internal class OutgoingImageMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : MessageViewHolder(view) {

    companion object {
        private val TAG = OutgoingImageMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_image_message
    }

    private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
    private val textView = view.findViewById<KenesChatMessageTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    override fun bind(message: Message) {
        if (message.attachments.isNullOrEmpty()) {
            imageView.loadImage(message.media?.urlPath)
        } else {
            imageView.loadImage(message.attachments?.first()?.urlPath)
        }

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