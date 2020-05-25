package q19.kenes_widget.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import q19.kenes_widget.R
import q19.kenes_widget.util.FileUtil.getRootDirPath
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import kotlin.math.min

class DownloadFileTask(
    private val context: Context,
    private val filename: String,
    private val callback: (file: File) -> Unit
) : AsyncTask<String, Int, String>() {

    companion object {
        const val TAG = "DownloadFileTask"
        private const val CHANNEL_ID = 123
        private const val CHANNEL_NAME = "Kenes Widget Downloader"
    }

    private var notifyManager: NotificationManager? = null
    private var notificationBuilder: NotificationCompat.Builder? = null

    private var outputFile: File? = null

    private var outputStream: OutputStream? = null

    override fun onPreExecute() {
        super.onPreExecute()

        notifyManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID.toString())

        notificationBuilder?.setContentTitle("Download")
            ?.setContentText("Download in progress")
            ?.setAutoCancel(false)
            ?.setDefaults(0)
            ?.setSmallIcon(R.drawable.kenes_ic_file)

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID.toString(),
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Description"
            channel.setSound(null, null)
            channel.enableLights(false)
            channel.lightColor = Color.BLUE
            channel.enableVibration(false)
            notifyManager?.createNotificationChannel(channel)
        }

        notificationBuilder?.setProgress(100, 0, false)
        notifyManager?.notify(CHANNEL_ID, notificationBuilder?.build())
    }

    override fun doInBackground(vararg params: String?): String? {
        var count: Int
        try {
            val url = URL(params[0])

            val conection = url.openConnection()
            conection.connect()

            val lenghtOfFile = conection.contentLength

            val input = BufferedInputStream(url.openStream(), 8192)

            // Output stream
            outputStream = FileOutputStream(context.getRootDirPath() + "/" + filename)

            outputFile = File(context.getRootDirPath() + "/" + filename)

            val data = ByteArray(1024)
            var total = 0L

            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                val cur = (total * 100 / lenghtOfFile).toInt()
                publishProgress(min(cur, 100))
                if (min(cur, 100) > 98) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "Sleeping failure")
                    }
                }
                Log.i(TAG, "CurrentProgress: ${min(cur, 100)} - $cur")
                outputStream?.write(data, 0, count)
            }

            outputStream?.flush()
            outputStream?.close()
            input.close()
        } catch (e: Exception) {
            Log.e("Error: ", e.message ?: "")
        }
        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        values[0]?.let { notificationBuilder?.setProgress(100, it, false) }
        notifyManager?.notify(CHANNEL_ID, notificationBuilder?.build())
        super.onProgressUpdate(*values)
    }

    override fun onPostExecute(fileUrl: String?) {
        notificationBuilder?.setContentText("Download complete")

        outputFile?.let { callback(it) }

        notificationBuilder?.setProgress(0, 0, false)
        notifyManager?.notify(CHANNEL_ID, notificationBuilder?.build())
    }

}