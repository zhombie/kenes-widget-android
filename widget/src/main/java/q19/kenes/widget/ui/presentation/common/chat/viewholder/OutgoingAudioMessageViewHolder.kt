package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import android.widget.FrameLayout
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTextView
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class OutgoingAudioMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : MessageViewHolder(view) {

    companion object {
        private val TAG = OutgoingAudioMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_audio_message
    }

    private val indicatorView = view.findViewById<FrameLayout>(R.id.indicatorView)
    private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
    private val slider = view.findViewById<Slider>(R.id.slider)
    private val playTimeView = view.findViewById<MaterialTextView>(R.id.playTimeView)
    private val textView = view.findViewById<KenesChatMessageTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    override fun bind(message: Message) {
        titleView.text = message.media?.title

        playTimeView.text = "00:00"

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

        indicatorView.setOnClickListener {
            message.media?.let { media ->
                callback?.onAudioClicked(media, absoluteAdapterPosition)
            }
        }
    }

}