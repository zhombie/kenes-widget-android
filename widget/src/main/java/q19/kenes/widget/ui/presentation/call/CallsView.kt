package q19.kenes.widget.ui.presentation.call

import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface CallsView : BaseView {
    fun showCalls(anyCalls: List<AnyCall>)

    fun tryToResolvePermissions(call: Call)

    fun launchPendingCall(call: Call)
}