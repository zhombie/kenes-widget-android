package q19.kenes.widget.ui.presentation.platform

import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import kz.q19.common.locale.LocaleManager
import kz.q19.domain.model.language.Language
import q19.kenes.widget.di.Injection
import java.util.*

internal open class BaseFragment constructor(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    constructor() : this(0)

    protected val injection: Injection?
        get() = if (context == null) null else Injection.getInstance(requireContext())

    fun getCurrentLocale(): Locale? {
        return LocaleManager.getLocale()
    }

    fun getCurrentLanguage(): Language {
        val locale = getCurrentLocale()
        return locale?.let { Language.from(it) } ?: Language.DEFAULT
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