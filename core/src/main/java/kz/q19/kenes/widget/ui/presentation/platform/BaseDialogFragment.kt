package kz.q19.kenes.widget.ui.presentation.platform

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import kz.q19.common.locale.LocaleManager
import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.di.Injection
import java.util.*

internal abstract class BaseDialogFragment<Presenter : BasePresenter<*>> constructor(
    @LayoutRes open val contentLayoutId: Int
) : DialogFragment(contentLayoutId) {

    constructor() : this(0)

    internal val injection: Injection
        get() = Injection.getInstance(requireContext())

    protected lateinit var presenter: Presenter

    protected abstract fun createPresenter(): Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = createPresenter()
    }

    override fun onResume() {
        super.onResume()

        presenter.onViewResumed()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.detachView()
    }

    protected fun getCurrentLocale(): Locale? {
        return LocaleManager.getLocale()
    }

    fun getCurrentLanguage(): Language {
        val locale = getCurrentLocale()
        return locale?.let { Language.from(it) } ?: Language.DEFAULT
    }

}