package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.message.Message
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatBotView : BaseView {
    // UI actions
    fun hideLoadingIndicator()
    fun showLoadingIndicator()

    // UI bind data
    fun showResponses(nestables: List<Nestable>)

    fun addNewMessage(message: Message)

    // Interaction actions
    fun copyHTMLText(label: String, text: CharSequence?, htmlText: String)
    fun share(title: String, text: CharSequence?, htmlText: String)

    fun toggleBottomSheet()

    fun clearMessageInputViewText()

    // Alert messages
    fun showNoResponsesFoundMessage()
}