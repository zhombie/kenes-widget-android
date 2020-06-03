package q19.kenes_widget.network.file

import android.content.Context
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.FileAsyncHttpResponseHandler
import com.loopj.android.http.RequestHandle
import cz.msebera.android.httpclient.Header
import q19.kenes_widget.R
import java.io.File

internal fun AsyncHttpClient.downloadFile(
    context: Context,
    file: File,
    url: String,
    listener: (DownloadResult) -> Unit
): RequestHandle? {
    return get(url, object : FileAsyncHttpResponseHandler(file) {
        override fun onProgress(bytesWritten: Long, totalSize: Long) {
            super.onProgress(bytesWritten, totalSize)

            val progress = (100 * bytesWritten / totalSize).toInt()
            listener(DownloadResult.Progress(progress))
        }

        override fun onSuccess(statusCode: Int, headers: Array<out Header>?, file: File?) {
            listener(DownloadResult.Success)
        }

        override fun onFailure(
            statusCode: Int,
            headers: Array<out Header>?,
            throwable: Throwable?,
            file: File?
        ) {
            listener(DownloadResult.Error(context.getString(R.string.kenes_file_download_error)))
        }
    })
}