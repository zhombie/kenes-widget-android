package kz.q19.kenes.widget.ui.presentation.info

import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.ui.platform.BasePresenter

internal class InfoPresenter constructor(
    private val language: Language
): BasePresenter<InfoView>() {

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
    }

}