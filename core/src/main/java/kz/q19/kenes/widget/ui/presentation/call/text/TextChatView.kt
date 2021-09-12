package kz.q19.kenes.widget.ui.presentation.call.text

import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.ui.presentation.platform.BaseView

internal interface TextChatView : BaseView {
    fun showNewMessage(message: Message)

    fun clearMessageInput()
}