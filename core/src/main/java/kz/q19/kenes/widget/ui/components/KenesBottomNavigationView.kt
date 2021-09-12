package kz.q19.kenes.widget.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.*
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.ui.presentation.common.Screen
import kz.q19.kenes.widget.util.DebouncedOnClickListener
import kz.q19.utils.android.dp2Px
import kz.q19.utils.drawable.withTint
import kz.q19.utils.textview.getCompoundDrawableOnTop
import kz.q19.utils.textview.showCompoundDrawableOnTop
import kotlin.math.roundToInt

internal class KenesBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = KenesBottomNavigationView::class.java.simpleName

        private val DEFAULT_ACTIVE_NAVIGATION_BUTTON_INDEX: Int = Screen.HOME.index
    }

    private var homeButton: AppCompatButton? = null
    private var callsButton: AppCompatButton? = null
    private var servicesButton: AppCompatButton? = null
    private var infoButton: AppCompatButton? = null

    private val navigationButtons: MutableList<AppCompatButton> = mutableListOf()

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
            screen = Screen.HOME,
            compoundDrawable = R.drawable.kenes_ic_home_gray,
            title = R.string.kenes_home
        ).also {
            addView(it)
            navigationButtons.add(Screen.HOME.index, it)
        }

        // Calls
        callsButton = buildNavigationButton(
            screen = Screen.CALLS,
            compoundDrawable = R.drawable.kenes_ic_headphones_gray,
            title = R.string.kenes_calls
        ).also {
            addView(it)
            navigationButtons.add(Screen.CALLS.index, it)
        }

        // Services
        servicesButton = buildNavigationButton(
            screen = Screen.SERVICES,
            compoundDrawable = R.drawable.kenes_ic_service_gray,
            title = R.string.kenes_services
        ).also {
            addView(it)
            navigationButtons.add(Screen.SERVICES.index, it)
        }

        // Info
        infoButton = buildNavigationButton(
            screen = Screen.INFO,
            compoundDrawable = R.drawable.kenes_ic_list_gray,
            title = R.string.kenes_info
        ).also {
            addView(it)
            navigationButtons.add(Screen.INFO.index, it)
        }

        activeNavigationButtonIndex = DEFAULT_ACTIVE_NAVIGATION_BUTTON_INDEX
    }

    private fun buildNavigationButton(
        screen: Screen,
        @DrawableRes compoundDrawable: Int,
        @StringRes title: Int
    ): AppCompatButton {
        return with(AppCompatButton(context)) {
            id = ViewCompat.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).also {
                it.weight = 1F
            }
            backgroundSelectableItemBackgroundBorderless()
            gravity = Gravity.CENTER
            showCompoundDrawableOnTop(compoundDrawable, padding = 6F.dp2Px().roundToInt())
            ellipsize = TextUtils.TruncateAt.END
            isAllCaps = true
            setTextColor(ContextCompat.getColor(context, R.color.kenes_black_with_opacity_80))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8F)
            setPadding(0, 7F.dp2Px().roundToInt(), 0, 7F.dp2Px().roundToInt())
            setText(title)
            setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            setOnClickListener(object : DebouncedOnClickListener() {
                override fun onDebouncedClick(v: View) {
                    dispatchSelection(screen)

                    setNavigationButtonActive(screen)
                }
            })
            return@with this
        }
    }

    fun setFirstNavigationButtonActive() {
        val firstNavigationButton = getFirstNavigationButton()
        if (firstNavigationButton != null) {
            dispatchSelection(firstNavigationButton)

            setNavigationButtonActive(firstNavigationButton)
        }
    }

    private fun getFirstNavigationButton(): Screen? {
        return when {
            isNavigationButtonFirst(homeButton) -> Screen.HOME
            isNavigationButtonFirst(callsButton) -> Screen.CALLS
            isNavigationButtonFirst(servicesButton) -> Screen.SERVICES
            isNavigationButtonFirst(infoButton) -> Screen.INFO
            else -> null
        }
    }

    private fun isNavigationButtonFirst(button: AppCompatButton?): Boolean {
        if (navigationButtons.isNullOrEmpty()) return false
        return navigationButtons.isNotEmpty() && navigationButtons.first() == button
    }

    private fun setNavigationButtonActive(screen: Screen): Boolean {
        return when (screen) {
            Screen.HOME -> setActiveNavigationButton(homeButton)
            Screen.CALLS -> setActiveNavigationButton(callsButton)
            Screen.SERVICES -> setActiveNavigationButton(servicesButton)
            Screen.INFO -> setActiveNavigationButton(infoButton)
        }
    }

    private fun setActiveNavigationButton(button: AppCompatButton?): Boolean {
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
            navigationButtons.forEachIndexed { eachIndex, appCompatButton ->
                if (index == eachIndex) {
                    setActiveNavigationButtonTint(navigationButtons[index])
                } else {
                    setInactiveNavigationButtonTint(appCompatButton)
                }
            }
        }
    }

    private fun setActiveNavigationButtonTint(button: AppCompatButton?) {
        button.setTint(R.color.kenes_blue)
        button?.animate()
            ?.setDuration(125L)
            ?.scaleX(0.95F)
            ?.scaleY(0.95F)
            ?.start()
    }

    private fun setInactiveNavigationButtonTint(button: AppCompatButton?) {
        button.setTint(R.color.kenes_black_with_opacity_80)
        button?.animate()
            ?.setDuration(125L)
            ?.scaleX(1.0F)
            ?.scaleY(1.0F)
            ?.start()
    }

    private fun AppCompatButton?.setTint(@ColorRes colorResId: Int): Boolean {
        if (this == null) return false

        val color = ContextCompat.getColor(context, colorResId)

        getCompoundDrawableOnTop()?.withTint(color)

        setTextColor(color)

        return textColors == ContextCompat.getColorStateList(context, colorResId)
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun showNavigationButton(screen: Screen) {
        val appCompatButton = when (screen) {
            Screen.HOME -> homeButton
            Screen.CALLS -> callsButton
            Screen.SERVICES -> servicesButton
            Screen.INFO -> infoButton
        }

        appCompatButton.showNavigationButton(
            index = screen.index,
            listener = object : DebouncedOnClickListener() {
                override fun onDebouncedClick(v: View) {
                    dispatchSelection(screen)

                    setNavigationButtonActive(screen)
                }
            }
        )
    }

    fun hideNavigationButton(screen: Screen) {
        Logger.debug(TAG, "hideNavButton() -> screen: $screen")
        when (screen) {
            Screen.HOME -> homeButton.hideNavigationButton()
            Screen.CALLS -> callsButton.hideNavigationButton()
            Screen.SERVICES -> servicesButton.hideNavigationButton()
            Screen.INFO -> infoButton.hideNavigationButton()
        }
    }

    private fun AppCompatButton?.showNavigationButton(
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

    private fun AppCompatButton?.hideNavigationButton() {
        if (this != null) {
            setOnClickListener(null)
            if (navigationButtons.contains(this)) {
                navigationButtons.remove(this)
            }
        }
    }

    private fun AppCompatButton.backgroundSelectableItemBackgroundBorderless() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.selectableItemBackgroundBorderless,
            outValue,
            true
        )
        setBackgroundResource(outValue.resourceId)
    }

    private fun dispatchSelection(screen: Screen) {
        if (screen.index == activeNavigationButtonIndex) {
            callback?.onBottomNavigationReselected(screen)
        } else {
            callback?.onBottomNavigationSelected(screen)
        }
    }

    interface Callback {
        fun onBottomNavigationSelected(screen: Screen)
        fun onBottomNavigationReselected(screen: Screen)
    }

}