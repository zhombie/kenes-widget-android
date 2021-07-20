package q19.kenes.widget.ui.presentation.home

import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatBotView : BaseView {
    // UI actions
    fun hideLoadingIndicator()
    fun showLoadingIndicator()

    // UI bind data
    fun showResponses(nestables: List<Nestable>)

    // Interaction actions
    fun copyHTMLText(label: String, text: CharSequence?, htmlText: String)
    fun share(title: String, text: CharSequence?, htmlText: String)

    // Alert messages
    fun showNoResponsesFoundMessage()
}