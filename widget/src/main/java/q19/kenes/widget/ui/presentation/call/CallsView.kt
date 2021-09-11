package q19.kenes.widget.ui.presentation.call

import kz.q19.domain.model.call.AnyCall
import kz.q19.domain.model.call.Call
import q19.kenes.widget.ui.presentation.call.selection.CallSelection
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface CallsView : BaseView {
    fun showCalls(anyCalls: List<AnyCall>)

    fun showCallSelection(callSelection: CallSelection)

    fun tryToResolvePermissions(call: Call)

    fun launchCall(call: Call)
}