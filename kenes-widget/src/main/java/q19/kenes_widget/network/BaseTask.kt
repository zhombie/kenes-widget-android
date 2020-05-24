package q19.kenes_widget.network

internal interface BaseTask<T> {
    val tag: String

    fun run(): T?
}