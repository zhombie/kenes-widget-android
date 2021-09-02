package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTextView
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.util.formatToDigitalClock
import q19.kenes.widget.util.loadImage
import q19.kenes_widget.R

internal class OutgoingVideoMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : MessageViewHolder(view) {

    companion object {
        private val TAG = OutgoingVideoMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_video_message
    }

    private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)
    private val durationView = view.findViewById<MaterialTextView>(R.id.durationView)
    private val playButton = view.findViewById<ShapeableImageView>(R.id.playButton)
    private val textView = view.findViewById<KenesChatMessageTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    override fun bind(message: Message) {
        imageView.loadImage(message.media?.urlPath)

        durationView.text = message.media?.duration?.formatToDigitalClock()

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

        imageView.setOnClickListener {
            message.media?.let { media ->
                callback?.onVideoClicked(imageView, media)
            }
        }
    }

}