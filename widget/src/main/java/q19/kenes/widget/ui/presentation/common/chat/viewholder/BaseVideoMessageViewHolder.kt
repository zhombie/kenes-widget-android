package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTextView
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.util.formatToDigitalClock
import q19.kenes_widget.R

internal abstract class BaseVideoMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseMessageViewHolder(view) {

    protected val imageView: ShapeableImageView? = view.findViewById(R.id.imageView)
    protected val durationView: MaterialTextView? = view.findViewById(R.id.durationView)
    protected val playButton: ShapeableImageView? = view.findViewById(R.id.playButton)
    protected val textView: KenesChatMessageTextView? = view.findViewById(R.id.textView)
    protected val timeView: KenesChatMessageTimeView? = view.findViewById(R.id.timeView)

    override fun bind(message: Message) {
        imageView?.load(message.media?.urlPath)

        durationView?.text = message.media?.duration?.formatToDigitalClock()

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
                callback?.onVideoClicked(imageView, media)
            }
        }
    }

}