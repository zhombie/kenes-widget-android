package q19.kenes.widget.ui.presentation.platform

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
import kz.q19.domain.model.language.Language
import java.util.*

open class BaseFragment constructor(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    constructor() : this(0)

    protected var menu: Menu? = null

    protected var toast: Toast? = null

    override fun onDestroy() {
        menu = null

        toast?.cancel()
        toast = null
        super.onDestroy()
    }

    fun getCurrentLocale(): Locale? {
        val activity = activity
        return if (activity is BaseActivity) {
            activity.getCurrentLocale()
        } else {
            null
        }
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
                toast?.cancel()
                toast = null
                toast = Toast.makeText(context, text, duration)
                toast?.show()
            }
        }
    }

    fun toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            val context = context ?: return
            if (duration == Toast.LENGTH_SHORT || duration == Toast.LENGTH_LONG) {
                toast?.cancel()
                toast = null
                toast = Toast.makeText(context, resId, duration)
                toast?.show()
            }
        }
    }

}