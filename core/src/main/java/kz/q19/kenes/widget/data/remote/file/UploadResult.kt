package kz.q19.kenes.widget.data.remote.file

internal sealed class UploadResult {
    data class Success constructor(
        val hash: String,
        val title: String?,
        val urlPath: String
    ) : UploadResult()

    data class Error constructor(
        val message: String? = null,
        val cause: Throwable? = null
    ) : UploadResult()

    data class Progress constructor(val progress: Int) : UploadResult()
}