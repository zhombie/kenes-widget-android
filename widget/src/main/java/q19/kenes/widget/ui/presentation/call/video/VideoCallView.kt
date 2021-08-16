package q19.kenes.widget.ui.presentation.call.video

import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface VideoCallView : BaseView {
    fun showCallAgentInfo(fullName: String, photoUrl: String?)
    fun showNewMessage(message: Message)
}