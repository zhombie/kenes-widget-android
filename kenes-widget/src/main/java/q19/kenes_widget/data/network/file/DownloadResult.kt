package q19.kenes_widget.data.network.file

internal sealed class DownloadResult {
    object Success : DownloadResult()

    data class Error constructor(
        val message: String? = null,
        val cause: Exception? = null
    ) : DownloadResult()

    data class Progress constructor(val progress: Int) : DownloadResult()
}