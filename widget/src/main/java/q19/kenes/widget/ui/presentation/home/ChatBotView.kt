package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.knowledge_base.ResponseInfo
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatBotView : BaseView {
    fun showResponseGroups(responseGroups: List<ResponseGroup>)
    fun showResponseInfo(responseInfo: ResponseInfo)
}