package q19.kenes.widget.ui.presentation.model

import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup

internal class ChatBot {

    var primaryResponseGroups: List<ResponseGroup>? = null

    val breadcrumb: MutableList<Nestable> = mutableListOf()

    var lastResponseGroupsLoadedTime: Long = -1L

    var isBottomSheetExpanded: Boolean = false

}