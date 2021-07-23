package q19.kenes.widget.ui.presentation.platform

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.LocaleManagerAppCompatDelegate
import androidx.lifecycle.Lifecycle
import kz.q19.common.locale.LocaleManager
import kz.q19.common.locale.utils.ActivityRecreationHelper
import kz.q19.domain.model.language.Language
import q19.kenes.widget.di.Injection
import java.util.*

internal open class BaseActivity : AppCompatActivity() {

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }

    internal val injection: Injection
        get() = Injection.getInstance(this)
    
    private var localeManagerAppCompatDelegate: LocaleManagerAppCompatDelegate? = null

    override fun getDelegate(): AppCompatDelegate {
        if (localeManagerAppCompatDelegate == null) {
            localeManagerAppCompatDelegate = LocaleManagerAppCompatDelegate(super.getDelegate())
        }
        return requireNotNull(localeManagerAppCompatDelegate)
    }

    override fun onResume() {
        super.onResume()
        ActivityRecreationHelper.onResume(this)
    }

    fun getCurrentLocale(): Locale? {
        return LocaleManager.getLocale()
    }

    fun getCurrentLanguage(): Language {
        val locale = getCurrentLocale()
        return locale?.let { Language.from(it) } ?: Language.DEFAULT
    }

    fun setLocale(locale: Locale) {
        LocaleManager.setLocale(locale)
        ActivityRecreationHelper.recreate(this, true)
    }

    override fun onDestroy() {
        super.onDestroy()

        ActivityRecreationHelper.onDestroy(this)
    }

    fun toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                Toast.makeText(this, text, duration).show()
            }
        }
    }

    fun toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                Toast.makeText(this, resId, duration).show()
            }
        }
    }

}