package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.ui.components.KenesChatMessageTextView
import kz.q19.kenes.widget.ui.components.KenesChatMessageTimeView
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.R

internal class IncomingRichContentMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseMessageViewHolder(view) {

    companion object {
        private val TAG = IncomingRichContentMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.kenes_cell_incoming_text_message
    }

    private val textView = view.findViewById<KenesChatMessageTextView>(R.id.textView)
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

            textView.visibility = View.VISIBLE
        }

        timeView.text = message.time
    }

}