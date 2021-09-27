package kz.q19.kenes.widget.ui.platform

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.os.HandlerCompat
import androidx.lifecycle.Lifecycle
import kz.q19.common.locale.LocaleManager
import kz.q19.domain.model.language.Language
import kz.q19.kenes.widget.di.Injection
import java.util.*

internal abstract class BaseFragment<Presenter : BasePresenter<*>> constructor(
    @LayoutRes contentLayoutId: Int
) : BaseResourceFragment(contentLayoutId) {

    constructor() : this(0)

    protected val injection: Injection
        get() = Injection.getInstance(requireContext())

    protected lateinit var presenter: Presenter

    protected abstract fun createPresenter(): Presenter

    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = HandlerCompat.createAsync(activity?.mainLooper ?: Looper.getMainLooper())

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

    fun runOnUiThread(action: Runnable) {
        handler?.post(action)
    }

    protected fun getCurrentLocale(): Locale? {
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