package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.knowledge_base.Nestable
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.presentation.platform.BaseView

internal interface ChatbotView : BaseView {
    // UI actions
    fun showLoadingIndicator()
    fun hideLoadingIndicator()

    // UI bind data
    fun showResponses(nestables: List<Nestable>)

    fun showNewChatMessage(message: Message)

    // Interactions
    fun copyHTMLText(label: String, text: CharSequence?, htmlText: String)
    fun share(title: String, text: CharSequence?, htmlText: String)

    fun collapseBottomSheet()
    fun expandBottomSheet()

    fun clearMessageInput()

    fun hideChatMessagesHeader()

    // Alert messages
    fun showNoResponsesFoundMessage()
}