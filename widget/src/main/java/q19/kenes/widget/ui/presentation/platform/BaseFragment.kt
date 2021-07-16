package q19.kenes.widget.ui.presentation.platform

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.Menu
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import kz.q19.common.locale.LocaleManager
import kz.q19.domain.model.language.Language
import q19.kenes.widget.di.Injection
import java.util.*

internal open class BaseFragment constructor(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    constructor() : this(0)

    internal val Context?.injection: Injection?
        get() = if (context == null) null else Injection.getInstance(requireContext())

    protected var menu: Menu? = null

    override fun onDestroy() {
        super.onDestroy()

        menu = null
    }

    fun getCurrentLocale(): Locale? {
        return LocaleManager.getLocale()
    }

    fun getCurrentLanguage(): Language {
        val locale = getCurrentLocale()
        return locale?.let { Language.from(it) } ?: Language.DEFAULT
    }

    fun getColorCompat(@ColorRes resId: Int): Int {
        return ContextCompat.getColor(requireContext(), resId)
    }

    fun getColorStateListCompat(@ColorRes resId: Int): ColorStateList? {
        return ContextCompat.getColorStateList(requireContext(), resId)
    }

    fun getDrawableCompat(@DrawableRes resId: Int): Drawable? {
        return AppCompatResources.getDrawable(requireContext(), resId)
    }

    fun toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            val context = context ?: return
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                Toast.makeText(context, text, duration).show()
            }
        }
    }

    fun toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            val context = context ?: return
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                Toast.makeText(context, resId, duration).show()
            }
        }
    }

}