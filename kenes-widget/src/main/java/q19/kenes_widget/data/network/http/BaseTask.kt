package q19.kenes_widget.data.network.http

interface BaseTask<T> {
    @Suppress("PropertyName")
    val TAG: String

    fun execute(): T?
}