package kz.q19.kenes.widget

import android.app.Application
import android.content.res.Configuration
import kz.q19.kenes.widget.api.LocaleManager

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        LocaleManager.initialize(applicationContext, KenesWidget.SUPPORTED_LOCALES)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        LocaleManager.onConfigurationChanged()
    }

}