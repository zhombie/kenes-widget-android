package q19.kenes.widget.ui.presentation.info

import kz.q19.domain.model.language.Language
import q19.kenes.widget.ui.presentation.platform.BasePresenter

internal class InfoPresenter constructor(
    private val language: Language
): BasePresenter<InfoView>() {

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
    }

}