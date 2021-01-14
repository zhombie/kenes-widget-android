package q19.kenes_widget.core.errors

internal class ViewHolderViewTypeException(private val viewType: Int) : RuntimeException() {

    override val message: String
        get() = "There is no ViewHolder for viewType: $viewType"

}