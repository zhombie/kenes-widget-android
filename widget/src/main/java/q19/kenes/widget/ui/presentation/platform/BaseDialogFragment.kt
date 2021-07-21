package q19.kenes.widget.ui.presentation.platform

import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import kz.q19.common.locale.LocaleManager
import kz.q19.domain.model.language.Language
import q19.kenes.widget.di.Injection
import java.util.*

internal open class BaseDialogFragment constructor(
    @LayoutRes contentLayoutId: Int
) : DialogFragment(contentLayoutId) {

    constructor() : this(0)

    internal val injection: Injection?
        get() = if (context == null) null else Injection.getInstance(requireContext())

    fun getCurrentLocale(): Locale? {
        return LocaleManager.getLocale()
    }

    fun getCurrentLanguage(): Language {
        val locale = getCurrentLocale()
        return locale?.let { Language.from(it) } ?: Language.DEFAULT
    }

}