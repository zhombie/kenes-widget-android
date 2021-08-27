package q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.message.Message
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.presentation.common.BottomSheetState

internal class ChatbotInteractor {

    // UI
    var bottomSheetState: BottomSheetState = BottomSheetState.COLLAPSED

    var primaryResponseGroups: List<ResponseGroup> = emptyList()

    val breadcrumb: MutableList<Nestable> = mutableListOf()

    var lastResponseGroupsLoadedTime: Long = -1L

    val chatMessages = mutableListOf<Message>()

}