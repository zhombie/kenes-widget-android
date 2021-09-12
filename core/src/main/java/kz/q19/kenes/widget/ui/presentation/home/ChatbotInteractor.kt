package kz.q19.kenes.widget.ui.presentation.home

import kz.q19.domain.model.knowledge_base.Nestable
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState

internal class ChatbotInteractor {

    // UI
    var bottomSheetState: BottomSheetState = BottomSheetState.COLLAPSED

    var primaryResponseGroups: List<ResponseGroup> = emptyList()

    val breadcrumb: MutableList<Nestable> = mutableListOf()

    var lastResponseGroupsLoadedTime: Long = -1L

    val chatMessages = mutableListOf<Message>()

}