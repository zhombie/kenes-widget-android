package q19.kenes_widget

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity

@Keep
object KenesWidget {

    @Keep
    data class EntryParams(
        val hostname: String
    )

    @JvmStatic
    @Keep
    fun open(context: Context, entryParams: EntryParams): Intent =
        KenesWidgetV2Activity.newIntent(context, hostname = entryParams.hostname)

}