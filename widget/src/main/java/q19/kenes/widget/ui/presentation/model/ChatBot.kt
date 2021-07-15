package q19.kenes.widget.ui.presentation.model

import q19.kenes.widget.domain.model.ResponseGroup

internal class ChatBot {

    var activeResponseGroup: ResponseGroup? = null
    var activeResponseGroupChild: ResponseGroup.Child? = null

    var lastResponseGroupsLoadedTime: Long = -1L

    fun clear() {
        activeResponseGroup = null
        activeResponseGroupChild = null
    }

}