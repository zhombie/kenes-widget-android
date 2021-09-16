package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder.base

import android.view.View
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.ui.components.KenesChatMessageTextView
import kz.q19.kenes.widget.ui.components.KenesChatMessageTimeView
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter

internal abstract class BaseTextMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : BaseMessageViewHolder(view) {

    protected val textView: KenesChatMessageTextView? = view.findViewById(R.id.textView)
    protected val timeView: KenesChatMessageTimeView? = view.findViewById(R.id.timeView)

    override fun bind(message: Message) {
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