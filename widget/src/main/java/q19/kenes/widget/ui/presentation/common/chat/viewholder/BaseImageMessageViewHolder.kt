package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import com.google.android.material.imageview.ShapeableImageView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.components.KenesTextView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.util.loadStandardImage
import q19.kenes_widget.R

internal abstract class BaseImageMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseMessageViewHolder(view) {

    protected val imageView: ShapeableImageView? = view.findViewById(R.id.imageView)
    protected val textView: KenesTextView? = view.findViewById(R.id.textView)
    protected val timeView: KenesChatMessageTimeView? = view.findViewById(R.id.timeView)

    override fun bind(message: Message) {
        if (message.attachments.isNullOrEmpty()) {
            imageView?.loadStandardImage(message.media?.urlPath)
        } else {
            imageView?.loadStandardImage(message.attachments?.first()?.urlPath)
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

        imageView?.setOnClickListener {
            message.media?.let { media ->
                callback?.onImageClicked(imageView, media)
            }
        }
    }

}