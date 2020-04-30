package q19.kenes_widget.model

internal data class Configs(
    var contacts: MutableList<Contact> = mutableListOf(),
    var workingHours: WorkingHours = WorkingHours()
) {

    internal data class Contact(
        var id: String,
        var url: String
    )

    internal data class WorkingHours(
        var messageKk: String = "",
        var messageRu: String = ""
    ) {

        companion object {
            fun from(messageKk: String?, messageRu: String?): WorkingHours {
                return WorkingHours(messageKk ?: "", messageRu ?: "")
            }
        }

    }

    override fun toString(): String {
        return "$contacts"
    }

}