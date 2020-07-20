package q19.kenes_widget

import android.content.Context
import android.content.Intent

object KenesWidget {

    @JvmStatic
    fun open(context: Context, hostname: String): Intent {
        return KenesWidgetV2Activity.newIntent(context, hostname)
    }

}