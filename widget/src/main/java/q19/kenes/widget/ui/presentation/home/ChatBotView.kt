package q19.kenes.widget.ui.presentation.home

import q19.kenes.widget.domain.model.Response
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatBotView : BaseView {
    fun showResponseGroups(responseGroups: List<ResponseGroup>)
    fun showResponse(response: Response)
}