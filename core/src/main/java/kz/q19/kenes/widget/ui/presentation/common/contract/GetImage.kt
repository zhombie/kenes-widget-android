package kz.q19.kenes.widget.ui.presentation.common.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

internal class GetImage : ActivityResultContract<Any, Uri?>() {

    companion object {
        private val TAG = GetImage::class.java.simpleName
    }

    override fun createIntent(context: Context, input: Any?): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null
        return intent.data
    }

}