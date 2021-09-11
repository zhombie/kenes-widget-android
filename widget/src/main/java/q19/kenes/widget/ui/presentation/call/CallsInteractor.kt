package q19.kenes.widget.ui.presentation.call

import kz.q19.domain.model.call.AnyCall

internal class CallsInteractor {

    var anyCalls: List<AnyCall> = emptyList()

    val breadcrumb = mutableListOf<AnyCall>()

}