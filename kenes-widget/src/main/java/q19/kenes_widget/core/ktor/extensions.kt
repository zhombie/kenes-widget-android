package q19.kenes_widget.core.ktor

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.math.roundToInt

suspend fun HttpClient.downloadFile(file: File, url: String): Flow<DownloadResult> {
    return flow {
        val response = call {
            url(url)
            method = HttpMethod.Get
        }.response

        val contentLength = response.contentLength() ?: 0L
        val data = ByteArray(contentLength.toInt())
        var offset = 0
        do {
            val currentRead = response.content.readAvailable(data, offset, data.size)
            offset += currentRead
            val progress = (offset * 100f / data.size).roundToInt()
            emit(DownloadResult.Progress(progress))
        } while (currentRead > 0)
        response.close()
        if (response.status.isSuccess()) {
            file.writeBytes(data)
            emit(DownloadResult.Success)
        } else {
            emit(DownloadResult.Error("File not downloaded"))
        }
    }
}
