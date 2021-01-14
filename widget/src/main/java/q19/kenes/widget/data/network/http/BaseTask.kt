package q19.kenes.widget.data.network.http

internal interface BaseTask<T> {
    @Suppress("PropertyName")
    val TAG: String

    fun execute(): T?
}