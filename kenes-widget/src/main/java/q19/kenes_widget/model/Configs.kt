package q19.kenes_widget.model

internal data class Configs(
    var opponent: Opponent = Opponent(),
    var contacts: MutableList<Contact> = mutableListOf(),
    var workingHours: WorkingHours = WorkingHours()
) {

    data class Opponent(
        var name: String? = null,
        var secondName: String? = null,
        var avatarUrl: String? = null
    )

    data class Contact(
        var id: String,
        var url: String
    )

    data class WorkingHours(
        var messageKk: String? = null,
        var messageRu: String? = null
    )

}