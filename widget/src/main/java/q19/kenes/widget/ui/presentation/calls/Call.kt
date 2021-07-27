package q19.kenes.widget.ui.presentation.calls

internal open class AnyCall

internal sealed class Call private constructor(
    open val id: Long,
    open val title: String,
    open val topic: String? = null
) : AnyCall() {

    internal data class Text constructor(
        override val id: Long,
        override val title: String,
        override val topic: String? = null
    ) : Call(id, title, topic)

    internal data class Audio constructor(
        override val id: Long,
        override val title: String,
        override val topic: String? = null
    ) : Call(id, title, topic)

    internal data class Video constructor(
        override val id: Long,
        override val title: String,
        override val topic: String? = null
    ) : Call(id, title, topic)

}

internal sealed class CallGroup private constructor(
    open val id: Long,
    open val title: String,
    open val children: List<AnyCall> = emptyList()
) : AnyCall() {

    internal data class Primary constructor(
        override val id: Long,
        override val title: String,
        override val children: List<AnyCall> = emptyList()
    ) : CallGroup(id, title, children)

    internal data class Secondary constructor(
        override val id: Long,
        override val title: String,
        override val children: List<AnyCall> = emptyList()
    ) : CallGroup(id, title, children)

}
