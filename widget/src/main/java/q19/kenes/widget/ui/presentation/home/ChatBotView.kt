package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.configs.Configs
import q19.kenes.widget.ui.presentation.platform.BaseView

interface ChatBotView : BaseView {
    fun showMediaCalls(calls: List<Configs.Call>)
}