package q19.kenes_widget.data.network.file

sealed class DownloadResult {
    object Success : DownloadResult()
    data class Error(val message: String? = null, val cause: Exception? = null) : DownloadResult()
    data class Progress(val progress: Int) : DownloadResult()
}