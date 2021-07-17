package q19.kenes.widget.ui.presentation.home

import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseInfo
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatBotView : BaseView {
    fun showResponseGroups(responseGroups: List<Nestable>)
    fun showResponseInfo(responseInfo: ResponseInfo)
}