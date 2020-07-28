package q19.kenes_widget

import android.content.Context
import android.content.Intent
import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity

object KenesWidget {

    data class EntryParams(
        val hostname: String
    )

    @JvmStatic
    fun open(context: Context, entryParams: EntryParams): Intent =
        KenesWidgetV2Activity.newIntent(context, hostname = entryParams.hostname)

}