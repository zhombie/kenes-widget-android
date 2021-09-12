package kz.q19.kenes.widget.ui.presentation.platform

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.LocaleManagerAppCompatDelegate
import androidx.lifecycle.Lifecycle
import kz.q19.common.locale.LocaleManager
import kz.q19.common.locale.utils.ActivityRecreationHelper
import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.di.Injection
import java.util.*

internal abstract class BaseActivity<Presenter : BasePresenter<*>> : AppCompatActivity() {

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }

    internal val injection: Injection
        get() = Injection.getInstance(this)

    protected lateinit var presenter: Presenter

    protected abstract fun createPresenter(): Presenter

    private var localeManagerAppCompatDelegate: LocaleManagerAppCompatDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = createPresenter()
    }

    override fun getDelegate(): AppCompatDelegate {
        return localeManagerAppCompatDelegate
            ?: LocaleManagerAppCompatDelegate(super.getDelegate()).also {
                localeManagerAppCompatDelegate = it
            }
    }

    override fun onResume() {
        super.onResume()

        ActivityRecreationHelper.onResume(this)

        presenter.onViewResumed()
    }

    protected fun getCurrentLocale(): Locale? {
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

        presenter.detachView()
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