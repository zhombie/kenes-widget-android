package q19.kenes_widget.core.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList

/**
 * Created by Akexorcist on 10/19/2017 AD.
 */
class LocalizationContext(base: Context) : ContextWrapper(base) {
    override fun getResources(): Resources {
        val locale = LanguageSetting.getLanguageWithDefault(this, LanguageSetting.getDefaultLanguage(this))
        val configuration = super.getResources().configuration

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> configuration.setLocales(LocaleList(locale))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> configuration.setLocale(locale)
            else -> configuration.locale = locale
        }
        val metrics = super.getResources().displayMetrics
        return Resources(assets, metrics, configuration)
    }
}