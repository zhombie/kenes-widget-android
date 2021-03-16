package q19.kenes.widget.api

import android.content.Context
import kz.q19.common.locale.LocaleManager
import java.util.*

object LocaleManager {

    fun initialize(context: Context, locales: List<Locale>) {
        LocaleManager.initialize(context, locales)
    }

    fun onConfigurationChanged() {
        LocaleManager.onConfigurationChanged()
    }

}