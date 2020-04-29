package q19.kenes_widget

import android.os.AsyncTask

private class AsyncTask : AsyncTask<String, String, String>() {

    override fun doInBackground(vararg params: String?): String {
        return ""
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