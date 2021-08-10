package q19.kenes.widget.ui.presentation.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.HTMLTextView
import q19.kenes.widget.ui.components.MessageTimeView
import q19.kenes.widget.ui.presentation.chat.ChatMessagesAdapter
import q19.kenes_widget.R

internal class IncomingImageMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : RecyclerView.ViewHolder(view) {

    companion object {
        private val TAG = IncomingImageMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_incoming_text_message
    }

    private val textView = view.findViewById<HTMLTextView>(R.id.textView)
    private val timeView = view.findViewById<MessageTimeView>(R.id.timeView)

    fun bind(message: Message) {
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