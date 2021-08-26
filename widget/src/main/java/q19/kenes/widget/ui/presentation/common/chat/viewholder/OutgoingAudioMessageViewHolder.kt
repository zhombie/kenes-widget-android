package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.components.KenesTextView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class OutgoingAudioMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : MessageViewHolder(view) {

    companion object {
        private val TAG = OutgoingAudioMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_outgoing_text_message
    }

    private val textView = view.findViewById<KenesTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    override fun bind(message: Message) {
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

            textView.enableAutoLinkMask()
            textView.enableLinkMovementMethod()

            textView.visibility = View.VISIBLE
        }

        timeView.text = message.time
    }

}