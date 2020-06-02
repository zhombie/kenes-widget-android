package q19.kenes_widget.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import q19.kenes_widget.R
import q19.kenes_widget.util.FileUtil.getRootDirPath
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import kotlin.math.min

internal class DownloadFileTask(
    private val context: Context,
    private val filename: String,
    private val callback: ((file: File) -> Unit)? = null,
    private val progressListener: ((progress: Int) -> Unit)? = null
) : AsyncTask<String, Int, String>() {

    companion object {
        const val TAG = "DownloadFileTask"

        private const val NOTIFICATION_CHANNEL_ID = 123
    }

    @Suppress("PrivatePropertyName")
    private val NOTIFICATION_CHANNEL_NAME: String
        get() = context.getString(R.string.kenes_notification_channel_name)

    private var notifyManager: NotificationManager? = null
    private var notificationBuilder: NotificationCompat.Builder? = null

    private var outputFile: File? = null

    private var outputStream: OutputStream? = null

    private var progress: Int = 0
        set(value) {
            if (value == 0) {
                progressListener?.invoke(0)
                field = value
                return
            } else if (value == 100) {
                progressListener?.invoke(100)
                field = value
                return
            }
            if (field == value || field - value < 10) return
            field = value
            progressListener?.invoke(progress)
        }

    override fun onPreExecute() {
        super.onPreExecute()

        notifyManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID.toString())

        notificationBuilder?.setContentTitle(context.getString(R.string.kenes_file_download))
            ?.setContentText(filename)
            ?.setAutoCancel(false)
            ?.setDefaults(0)
            ?.setSmallIcon(R.drawable.kenes_ic_file_blue)

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID.toString(),
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(null, null)
            channel.enableLights(false)
            channel.lightColor = ContextCompat.getColor(context, R.color.kenes_blue)
            channel.enableVibration(false)
            notifyManager?.createNotificationChannel(channel)
        }

        notificationBuilder?.setProgress(100, 0, false)
        notifyManager?.notify(NOTIFICATION_CHANNEL_ID, notificationBuilder?.build())
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
            outputStream = FileOutputStream(context.getRootDirPath() + File.separatorChar + filename)

            outputFile = File(context.getRootDirPath() + File.separatorChar + filename)

            val data = ByteArray(1024)
            var total = 0L

            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                val cur = (total * 100 / lenghtOfFile).toInt()
                val progress = min(cur, 100)
                this.progress = progress
                publishProgress(progress)
                if (min(cur, 100) > 98) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "Sleeping failure")
                    }
                }
//                Log.i(TAG, "CurrentProgress: ${min(cur, 100)} - $cur")
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
        if (!values.isNullOrEmpty()) {
            val progress = values[0] ?: 0
//            progressListener?.invoke(progress)
            notificationBuilder?.setProgress(100, progress, false)
            notifyManager?.notify(NOTIFICATION_CHANNEL_ID, notificationBuilder?.build())
        }
        super.onProgressUpdate(*values)
    }

    override fun onPostExecute(fileUrl: String?) {
        notificationBuilder?.setContentText(context.getString(R.string.kenes_file_download_completed))

        progressListener?.invoke(100)
        notificationBuilder?.setProgress(0, 0, false)
        notifyManager?.notify(NOTIFICATION_CHANNEL_ID, notificationBuilder?.build())

        outputFile?.let { callback?.invoke(it) }
    }

}