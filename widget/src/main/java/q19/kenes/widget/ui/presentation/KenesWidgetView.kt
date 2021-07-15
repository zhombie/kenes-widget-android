package q19.kenes.widget.ui.presentation

import kz.q19.domain.model.configs.Configs
import q19.kenes.widget.ui.components.BottomNavigationView
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface KenesWidgetView : BaseView {
    fun showBotInfo(bot: Configs.Bot)

    fun navigateTo(index: Int)
}