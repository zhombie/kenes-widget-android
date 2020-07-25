package q19.kenes_widget

import android.content.Context
import android.content.Intent
import q19.kenes_widget.model.EntryParams
import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity

object KenesWidget {

    @JvmStatic
    fun open(context: Context, entryParams: EntryParams): Intent {
        return KenesWidgetV2Activity.newIntent(context, entryParams.hostname)
    }

}