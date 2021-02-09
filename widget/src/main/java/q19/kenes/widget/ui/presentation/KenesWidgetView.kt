package q19.kenes.widget.ui.presentation

import kz.q19.domain.model.configs.Configs
import q19.kenes.widget.ui.presentation.platform.BaseView

interface KenesWidgetView : BaseView {
    fun showBotInfo(bot: Configs.Bot)
}