package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.message.Message
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup

internal class ChatbotInteractor {

    var primaryResponseGroups: List<ResponseGroup>? = null

    val breadcrumb: MutableList<Nestable> = mutableListOf()

    var lastResponseGroupsLoadedTime: Long = -1L

    var isBottomSheetExpanded: Boolean = false

    val chatMessages = mutableListOf<Message>()

}