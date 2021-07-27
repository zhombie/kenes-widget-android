package q19.kenes.widget.ui.presentation.calls

internal class CallsInteractor {

    var anyCalls: List<AnyCall> = emptyList()

    val breadcrumb = mutableListOf<AnyCall>()

    var lastCall: Call? = null

    var isBottomSheetExpanded: Boolean = false

}