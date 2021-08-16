package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.message.Message
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatbotView : BaseView {
    // UI actions
    fun hideLoadingIndicator()
    fun showLoadingIndicator()

    // UI bind data
    fun showResponses(nestables: List<Nestable>)

    fun showNewMessage(message: Message)

    // Interactions
    fun copyHTMLText(label: String, text: CharSequence?, htmlText: String)
    fun share(title: String, text: CharSequence?, htmlText: String)

    fun toggleBottomSheet()

    fun clearMessageInput()

    fun hideChatMessagesHeader()

    // Alert messages
    fun showNoResponsesFoundMessage()
}