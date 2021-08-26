package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.components.KenesTextView
import q19.kenes_widget.R

internal class NotificationMessageViewHolder constructor(view: View) : MessageViewHolder(view) {

    companion object {
        private val TAG = NotificationMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_notification_message
    }

    private val textView = view.findViewById<KenesTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    override fun bind(message: Message) {
        textView.text = message.text

        timeView.text = message.time
    }

}