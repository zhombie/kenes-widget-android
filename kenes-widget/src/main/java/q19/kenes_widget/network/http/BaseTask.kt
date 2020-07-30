package q19.kenes_widget.network.http

interface BaseTask<T> {
    @Suppress("PropertyName")
    val TAG: String

    fun run(): T?
}