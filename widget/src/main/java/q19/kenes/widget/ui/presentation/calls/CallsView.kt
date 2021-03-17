package q19.kenes.widget.ui.presentation.calls

import kz.q19.domain.model.configs.Configs
import q19.kenes.widget.ui.presentation.platform.BaseView

interface CallsView : BaseView {
    fun showMediaCalls(calls: List<Configs.Call>)

    fun launchVideoCall(call: Call)
}