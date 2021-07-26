package q19.kenes.widget.ui.presentation.calls

internal sealed class Call constructor(
    open val id: Long,
    open val title: String
) {

    internal data class Text constructor(
        override val id: Long,
        override val title: String
    ) : Call(id, title)

    internal data class Audio constructor(
        override val id: Long,
        override val title: String
    ) : Call(id, title)

    internal data class Video constructor(
        override val id: Long,
        override val title: String
    ) : Call(id, title)

}

internal open class CallGroup constructor(
    override val id: Long,
    override val title: String,
    open val children: List<Call> = emptyList()
) : Call(id, title) {

    fun hasAudioCall(): Boolean {
        return children.any { it is Audio }
    }

    fun hasVideoCall(): Boolean {
        return children.any { it is Video }
    }

    fun hasAudioAndVideoCall(): Boolean {
        var hasAudioCall = false
        var hasVideoCall = false
        children.forEach {
            if (it is Audio) {
                hasAudioCall = true
            }
            if (it is Video) {
                hasVideoCall = true
            }
        }
        return hasAudioCall && hasVideoCall
    }

    override fun toString(): String {
        return "CallGroup(id=$id, title=$title, children=$children)"
    }

}

internal data class PrimaryCallGroup constructor(
    override val id: Long,
    override val title: String,
    override val children: List<Call> = emptyList()
) : CallGroup(id, title, children)



// ------------------

@Deprecated("")
internal data class OldCall constructor(
    val id: Long,
    val title: String,
    val isPrimary: Boolean = false,
    val children: List<OldCall> = emptyList()
)