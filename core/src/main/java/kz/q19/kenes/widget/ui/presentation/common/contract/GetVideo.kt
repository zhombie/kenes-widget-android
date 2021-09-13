package kz.q19.kenes.widget.ui.presentation.common.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import com.otaliastudios.transcoder.TranscoderListener
import kz.q19.kenes.widget.core.logging.Logger

internal class GetVideo : ActivityResultContract<Any, Uri?>(), TranscoderListener {

    companion object {
        private val TAG = GetVideo::class.java.simpleName
    }

    override fun createIntent(context: Context, input: Any?): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("video/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null
        return intent.data
    }

    override fun onTranscodeProgress(progress: Double) {
        Logger.debug(TAG, "onTranscodeProgress() -> $progress")
    }

    override fun onTranscodeCompleted(successCode: Int) {
        Logger.debug(TAG, "onTranscodeCompleted() -> $successCode")
    }

    override fun onTranscodeCanceled() {
        Logger.debug(TAG, "onTranscodeCanceled()")
    }

    override fun onTranscodeFailed(exception: Throwable) {
        Logger.debug(TAG, "onTranscodeFailed() -> $exception")
    }

}