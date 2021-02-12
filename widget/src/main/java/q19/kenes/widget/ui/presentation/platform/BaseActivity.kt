package q19.kenes.widget.ui.presentation.platform

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.LocaleManagerAppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import kz.q19.common.locale.LocaleManager
import kz.q19.common.locale.utils.ActivityRecreationHelper
import kz.q19.domain.model.language.Language
import java.util.*

open class BaseActivity : AppCompatActivity() {

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }

    protected var toast: Toast? = null
    protected var alertDialog: androidx.appcompat.app.AlertDialog? = null

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
        toast?.cancel()
        toast = null

        alertDialog?.dismiss()
        alertDialog = null

        super.onDestroy()

        ActivityRecreationHelper.onDestroy(this)
    }

    fun getColorCompat(@ColorRes resId: Int): Int {
        return ContextCompat.getColor(this, resId)
    }

    fun getColorStateListCompat(@ColorRes resId: Int): ColorStateList {
        return requireNotNull(ContextCompat.getColorStateList(this, resId)) { "Not found!" }
    }

    fun getDrawableCompat(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(this, resId)
    }

    fun toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                toast?.cancel()
                toast = null
                toast = Toast.makeText(this, text, duration)
                toast?.show()
            }
        }
    }

    fun toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                toast?.cancel()
                toast = null
                toast = Toast.makeText(this, resId, duration)
                toast?.show()
            }
        }
    }

}