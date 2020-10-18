package q19.kenes_widget.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.*

/**
 * Created by Akexorcist on 10/19/2017 AD.
 */
object LocalizationUtility {

    fun applyLocalizationContext(baseContext: Context): Context {
        val baseLocale = getLocaleFromConfiguration(baseContext.resources.configuration)
        val currentLocale = LanguageSetting.getLanguageWithDefault(
            baseContext,
            LanguageSetting.getDefaultLanguage(baseContext)
        )
        if (!baseLocale.toString().equals(currentLocale.toString(), ignoreCase = true)) {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val context = LocalizationContext(baseContext)
                    val config = context.resources.configuration
                    config.setLocale(currentLocale)
                    val localeList = LocaleList(currentLocale)
                    LocaleList.setDefault(localeList)
                    config.setLocales(localeList)
                    context.createConfigurationContext(config)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                    val context = LocalizationContext(baseContext)
                    val config = context.resources.configuration
                    config.setLocale(currentLocale)
                    context.createConfigurationContext(config)
                }
                else -> {
                    val config = baseContext.resources.configuration
                    @Suppress("DEPRECATION")
                    config.locale = currentLocale
                    @Suppress("DEPRECATION")
                    baseContext.resources.updateConfiguration(
                        config,
                        baseContext.resources.displayMetrics
                    )
                    baseContext
                }
            }
        } else {
            return baseContext
        }
    }

    private fun getLocaleFromConfiguration(configuration: Configuration): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

}