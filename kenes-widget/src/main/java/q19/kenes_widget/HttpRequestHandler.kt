package q19.kenes_widget

import android.os.AsyncTask
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

internal class HttpRequestHandler(
    private val method: HttpMethod = HttpMethod.GET,
    private val url: String
) : AsyncTask<String, String, String>() {

    companion object {
        const val READ_TIMEOUT = 1000
        const val CONNECTION_TIMEOUT = 2000
    }

    enum class HttpMethod(val method: String) {
        GET("GET"),
        POST("POST")
    }

    override fun doInBackground(vararg params: String?): String {
        try {
            val configsUrl = URL(url)

            val connection = configsUrl.openConnection() as HttpURLConnection

            connection.requestMethod = method.method
            connection.readTimeout = READ_TIMEOUT
            connection.connectTimeout = CONNECTION_TIMEOUT

            connection.connect()

            val inputStreamReader = InputStreamReader(connection.inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder = StringBuilder()

            var inputLine: String?
            while (bufferedReader.readLine().also { inputLine = it } != null) {
                stringBuilder.append(inputLine)
            }

            bufferedReader.close()
            inputStreamReader.close()

            return stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun onProgressUpdate(vararg values: String?) {
        super.onProgressUpdate(*values)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
    }

    override fun onCancelled() {
        super.onCancelled()
    }

    override fun onCancelled(result: String?) {
        super.onCancelled(result)
    }

}