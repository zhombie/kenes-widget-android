package q19.kenes.widget.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import kz.q19.utils.android.dp2Px
import kz.q19.utils.textview.showCompoundDrawableOnTop
import q19.kenes.widget.util.DebouncedOnClickListener
import q19.kenes.widget.util.Logger
import q19.kenes.widget.util.getCompoundDrawableOnTop
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class BottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = BottomNavigationView::class.java.simpleName

        private val DEFAULT_ACTIVE_NAVIGATION_BUTTON_INDEX: Int = NavigationButton.HOME.index
    }

    enum class NavigationButton(val index: Int) {
        HOME(0),
        CALLS(1),
        SERVICES(2),
        INFO(3)
    }

    private var homeButton: MaterialButton? = null
    private var callsButton: MaterialButton? = null
    private var servicesButton: MaterialButton? = null
    private var infoButton: MaterialButton? = null

    private val navigationButtons: MutableList<MaterialButton> = mutableListOf()

    private var activeNavigationButtonIndex = DEFAULT_ACTIVE_NAVIGATION_BUTTON_INDEX
        set(value) {
            field = value
            updateActiveNavigationButtonTint(value)
        }

    var callback: Callback? = null

    private var isNavigationButtonsEnabled: Boolean = true
        set(value) {
            field = value
            setNavigationButtonEnabled(value)
        }

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.kenes_white))

        orientation = HORIZONTAL

        // Home
        homeButton = buildNavigationButton(
            navigationButton = NavigationButton.HOME,
            compoundDrawable = R.drawable.kenes_ic_home_gray,
            title = R.string.kenes_home
        ).also {
            addView(it)
            navigationButtons.add(NavigationButton.HOME.index, it)
        }

        // Calls
        callsButton = buildNavigationButton(
            navigationButton = NavigationButton.CALLS,
            compoundDrawable = R.drawable.kenes_ic_headphones_gray,
            title = R.string.kenes_calls
        ).also {
            addView(it)
            navigationButtons.add(NavigationButton.CALLS.index, it)
        }

        // Services
        servicesButton = buildNavigationButton(
            navigationButton = NavigationButton.SERVICES,
            compoundDrawable = R.drawable.kenes_ic_service_gray,
            title = R.string.kenes_services
        ).also {
            addView(it)
            navigationButtons.add(NavigationButton.SERVICES.index, it)
        }

        // Info
        infoButton = buildNavigationButton(
            navigationButton = NavigationButton.INFO,
            compoundDrawable = R.drawable.kenes_ic_list_gray,
            title = R.string.kenes_info
        ).also {
            addView(it)
            navigationButtons.add(NavigationButton.INFO.index, it)
        }

        activeNavigationButtonIndex = DEFAULT_ACTIVE_NAVIGATION_BUTTON_INDEX
    }

    private fun buildNavigationButton(
        navigationButton: NavigationButton,
        @DrawableRes compoundDrawable: Int,
        @StringRes title: Int
    ): MaterialButton {
        return with(MaterialButton(context)) {
            id = ViewCompat.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).also {
                it.weight = 1F
            }
            backgroundSelectableItemBackgroundBorderless()
            gravity = Gravity.CENTER
            showCompoundDrawableOnTop(compoundDrawable, padding = 7F.dp2Px().roundToInt())
            ellipsize = TextUtils.TruncateAt.END
            isAllCaps = true
            setTextColor(ContextCompat.getColor(context, R.color.kenes_dark_gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9F)
            setPadding(0, 7.5F.dp2Px().roundToInt(), 0, 7.5F.dp2Px().roundToInt())
            setText(title)
            setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            setOnClickListener(object : DebouncedOnClickListener() {
                override fun onDebouncedClick(v: View) {
                    updateActiveNavigationButtonTint(navigationButton.index)
                    callback?.onNavigationButtonClicked(navigationButton)
                }
            })
            return@with this
        }
    }

    fun getFirstNavigationButton(): NavigationButton? {
        return when {
            isNavigationButtonFirst(homeButton) -> NavigationButton.HOME
            isNavigationButtonFirst(callsButton) -> NavigationButton.CALLS
            isNavigationButtonFirst(servicesButton) -> NavigationButton.SERVICES
            isNavigationButtonFirst(infoButton) -> NavigationButton.INFO
            else -> null
        }
    }

    private fun isNavigationButtonFirst(button: MaterialButton?): Boolean {
        if (navigationButtons.isNullOrEmpty()) return false
        return navigationButtons.isNotEmpty() && navigationButtons.first() == button
    }

    fun setNavigationButtonActive(navigationButton: NavigationButton): Boolean {
        return when (navigationButton) {
            NavigationButton.HOME -> setActiveNavigationButton(homeButton)
            NavigationButton.CALLS -> setActiveNavigationButton(callsButton)
            NavigationButton.SERVICES -> setActiveNavigationButton(servicesButton)
            NavigationButton.INFO -> setActiveNavigationButton(infoButton)
        }
    }

    private fun setActiveNavigationButton(button: MaterialButton?): Boolean {
        if (button == null) return false
        val index = navigationButtons.indexOf(button)
        if (index >= 0) {
            activeNavigationButtonIndex = index
            return activeNavigationButtonIndex == index
        }
        return false
    }

    fun setNavigationButtonsEnabled(): Boolean {
        isNavigationButtonsEnabled = true
        return isNavigationButtonsEnabled
    }

    fun setNavigationButtonsDisabled(): Boolean {
        isNavigationButtonsEnabled = false
        return !isNavigationButtonsEnabled
    }

    private fun setNavigationButtonEnabled(isEnabled: Boolean): Boolean {
        navigationButtons.forEach { it.isEnabled = isEnabled }
        return navigationButtons.all { it.isEnabled == isEnabled }
    }

    private fun updateActiveNavigationButtonTint(index: Int) {
        if (index >= 0 && index < navigationButtons.size) {
            navigationButtons.forEach {
                setInactiveNavigationButtonTint(it)
            }
            setActiveNavigationButtonTint(navigationButtons[index])
        }
    }

    private fun setActiveNavigationButtonTint(button: MaterialButton?): Boolean {
        return button?.setTint(R.color.kenes_light_blue) == true
    }

    private fun setInactiveNavigationButtonTint(button: MaterialButton?): Boolean {
        return button?.setTint(R.color.kenes_dark_gray) == true
    }

    private fun MaterialButton?.setTint(@ColorRes colorResId: Int): Boolean {
        if (this == null) return false

        val compoundDrawable = getCompoundDrawableOnTop()
        val color = ContextCompat.getColor(context, colorResId)

        if (compoundDrawable != null) {
            val drawableWrap = DrawableCompat.wrap(compoundDrawable).mutate()
            DrawableCompat.setTint(drawableWrap, color)
        }

        setTextColor(color)

        return textColors == ContextCompat.getColorStateList(context, colorResId)
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun showNavigationButton(navigationButton: NavigationButton) {
        val appCompatButton = when (navigationButton) {
            NavigationButton.HOME -> homeButton
            NavigationButton.CALLS -> callsButton
            NavigationButton.SERVICES -> servicesButton
            NavigationButton.INFO -> infoButton
        }

        appCompatButton.showNavigationButton(navigationButton.index, object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                updateActiveNavigationButtonTint(navigationButton.index)
                callback?.onNavigationButtonClicked(navigationButton)
            }
        })
    }

    fun hideNavigationButton(navigationButton: NavigationButton) {
        Logger.debug(TAG, "hideNavButton() -> navigationButton: $navigationButton")
        when (navigationButton) {
            NavigationButton.HOME -> homeButton.hideNavigationButton()
            NavigationButton.CALLS -> callsButton.hideNavigationButton()
            NavigationButton.SERVICES -> servicesButton.hideNavigationButton()
            NavigationButton.INFO -> infoButton.hideNavigationButton()
        }
    }

    private fun MaterialButton?.showNavigationButton(
        index: Int,
        listener: DebouncedOnClickListener
    ) {
        if (this != null) {
            setOnClickListener(listener)
            if (!navigationButtons.contains(this)) {
                navigationButtons.add(index, this)
            }
        }
    }

    private fun MaterialButton?.hideNavigationButton() {
        if (this != null) {
            setOnClickListener(null)
            if (navigationButtons.contains(this)) {
                navigationButtons.remove(this)
            }
        }
    }

    private fun MaterialButton.backgroundSelectableItemBackgroundBorderless() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        setBackgroundResource(outValue.resourceId)
    }

    fun interface Callback {
        fun onNavigationButtonClicked(navigationButton: NavigationButton)
    }

}