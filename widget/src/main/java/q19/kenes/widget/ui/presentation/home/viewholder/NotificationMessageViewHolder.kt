package q19.kenes.widget.ui.presentation.home.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.HTMLTextView
import q19.kenes.widget.ui.components.MessageTimeView
import q19.kenes_widget.R

class NotificationMessageViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {

    companion object {
        private val TAG = NotificationMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_notification_message
    }

    private val textView = view.findViewById<HTMLTextView>(R.id.textView)
    private val timeView = view.findViewById<MessageTimeView>(R.id.timeView)

    fun bind(message: Message) {
        textView.text = message.text

        timeView.text = message.time
    }

}