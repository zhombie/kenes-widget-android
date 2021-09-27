package kz.q19.kenes.widget.ui.presentation.services

import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.ui.platform.BasePresenter

internal class ServicesPresenter constructor(
    private val language: Language
): BasePresenter<ServicesView>() {

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
    }

}