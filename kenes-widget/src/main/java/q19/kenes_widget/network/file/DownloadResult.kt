package q19.kenes_widget.network.file

internal sealed class DownloadResult {
    object Success : DownloadResult()
    data class Error(val message: String? = null, val cause: Exception? = null) : DownloadResult()
    data class Progress(val progress: Int) : DownloadResult()
}