package kz.q19.kenes.widget.ui.presentation.common.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toFile
import kz.q19.kenes.widget.domain.model.media.Content
import kz.q19.kenes.widget.domain.model.media.Image
import kz.q19.kenes.widget.util.ImageCompressor

internal class GetImage constructor(
    private val context: Context
): ActivityResultContract<Any, Image?>() {

    companion object {
        private val TAG = GetImage::class.java.simpleName

        private val PROJECTION = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        )
    }

    override fun createIntent(context: Context, input: Any?): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Image? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null

        val uri = intent.data ?: return null

        context.contentResolver
            ?.query(uri, PROJECTION, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                    val mimeType = context.contentResolver?.getType(uri)

                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

                    val compressed = ImageCompressor(context).compress(
                        uri,
                        Bitmap.CompressFormat.JPEG,
                        1280F,
                        1280F,
                        true,
                        75,
                        150F,
                        150F
                    )

                    if (compressed != null) {
                        val file = compressed.toFile()

                        return Image(
                            uri = uri,
                            displayName = displayName,
                            duplicateFile = Content.DuplicateFile(
                                mimeType = mimeType,
                                extension = extension,
                                file = file
                            )
                        )
                    }
                }
            }

        return null
    }

}