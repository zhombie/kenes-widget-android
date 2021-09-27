package kz.q19.kenes.widget.ui.presentation

import kz.q19.domain.model.configs.Configs
import kz.q19.kenes.widget.ui.platform.BaseView

internal interface KenesWidgetView : BaseView {
    fun showBotInfo(bot: Configs.Bot)

    fun showBottomSheetCloseButton()
    fun hideBottomSheetCloseButton()

    fun navigateTo(index: Int)
}