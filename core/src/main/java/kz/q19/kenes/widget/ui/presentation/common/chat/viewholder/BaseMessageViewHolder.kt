package kz.q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.message.Message

internal abstract class BaseMessageViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {

    abstract fun bind(message: Message)

}