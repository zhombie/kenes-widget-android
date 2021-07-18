package q19.kenes.widget.ui.presentation.model

import q19.kenes.widget.domain.model.Nestable

internal class ChatBot {

    val breadcrumb = mutableListOf<Nestable>()

    var lastResponseGroupsLoadedTime: Long = -1L

}