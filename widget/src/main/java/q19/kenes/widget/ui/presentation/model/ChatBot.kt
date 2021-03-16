package q19.kenes.widget.ui.presentation.model

import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup

internal class ChatBot {

    var activeResponseGroup: ResponseGroup? = null
    var activeResponse: Response? = null

    fun clear() {
        activeResponseGroup = null
        activeResponse = null
    }

}