package q19.kenes.widget.ui.presentation.calls.pending

import q19.kenes.widget.ui.presentation.calls.Call
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface PendingCallView : BaseView {
    fun showNoOnlineCallAgentsMessage(text: String?)

    fun navigateToHome()
    fun navigateToCall(call: Call)
}