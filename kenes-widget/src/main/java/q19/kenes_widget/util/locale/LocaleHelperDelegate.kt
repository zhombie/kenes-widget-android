package q19.kenes_widget.util.locale

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*

interface LocaleHelperActivityDelegate {
    fun setLocale(activity: Activity, newLocale: Locale)
    fun attachBaseContext(newBase: Context): Context
    fun applyOverrideConfiguration(
        baseContext: Context,
        overrideConfiguration: Configuration?
    ): Configuration?
    fun onPaused()
    fun onResumed(activity: Activity)
}

class LocaleHelperActivityDelegateImpl : LocaleHelperActivityDelegate {

    private var locale: Locale = Locale.getDefault()

    override fun setLocale(activity: Activity, newLocale: Locale) {
        LocaleHelper.setLocale(activity, newLocale)
        locale = newLocale
        activity.recreate()
    }

    override fun attachBaseContext(newBase: Context): Context = LocaleHelper.onAttach(newBase)

    override fun applyOverrideConfiguration(
        baseContext: Context,
        overrideConfiguration: Configuration?
    ): Configuration? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val uiMode = overrideConfiguration?.uiMode
            overrideConfiguration?.setTo(baseContext.resources.configuration)
            overrideConfiguration?.uiMode = uiMode
        }
        return overrideConfiguration
    }

    override fun onPaused() {
        locale = Locale.getDefault()
    }

    override fun onResumed(activity: Activity) {
        if (locale.language == Locale.getDefault().language) return

        activity.recreate()
    }

}