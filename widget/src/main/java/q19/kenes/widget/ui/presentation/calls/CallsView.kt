package q19.kenes.widget.ui.presentation.calls

import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface CallsView : BaseView {
    fun showMediaCalls(calls: List<Call>)

    fun launchCall(call: Call)
}