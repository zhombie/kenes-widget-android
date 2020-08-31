package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import q19.kenes_widget.R
import q19.kenes_widget.ui.presentation.model.BottomNavigation
import q19.kenes_widget.util.DebouncedOnClickListener
import q19.kenes_widget.util.Logger
import q19.kenes_widget.util.getCompoundDrawableOnTop

internal class BottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val TAG = "BottomNavigationView"
    }

    private var bottomNavigationView: LinearLayout? = null
    private var homeButton: AppCompatButton? = null
    private var operatorCallButton: AppCompatButton? = null
    private var servicesButton: AppCompatButton? = null
    private var infoButton: AppCompatButton? = null

    private val navButtons = mutableListOf<AppCompatButton>()

    private var activeNavButtonIndex = 0
        set(value) {
            field = value
            updateActiveNavButtonTintColor(value)
        }

    var callback: Callback? = null

    private var isNavButtonsEnabled: Boolean = true
        set(value) {
            field = value
            setIsNavButtonEnabled(value)
        }

    init {
        val view = inflate(context, R.layout.kenes_view_bottom_navigation, this)

        bottomNavigationView = view.findViewById(R.id.bottomNavigationView)
        homeButton = view.findViewById(R.id.homeButton)
        operatorCallButton = view.findViewById(R.id.operatorCallButton)
        servicesButton = view.findViewById(R.id.servicesButton)
        infoButton = view.findViewById(R.id.infoButton)
    }

    fun getFirstNavButton(): BottomNavigation? {
        return when {
            isNavButtonFirst(homeButton) -> BottomNavigation.HOME
            isNavButtonFirst(operatorCallButton) -> BottomNavigation.OPERATOR_CALL
            isNavButtonFirst(servicesButton) -> BottomNavigation.SERVICES
            isNavButtonFirst(infoButton) -> BottomNavigation.INFO
            else -> null
        }
    }

    private fun isNavButtonFirst(appCompatButton: AppCompatButton?): Boolean {
        return navButtons.isNotEmpty() && navButtons.first() == appCompatButton
    }

    fun setNavButtonActive(bottomNavigation: BottomNavigation) {
        when (bottomNavigation) {
            BottomNavigation.HOME -> setActiveNavButton(homeButton)
            BottomNavigation.OPERATOR_CALL -> setActiveNavButton(operatorCallButton)
            BottomNavigation.SERVICES -> setActiveNavButton(servicesButton)
            BottomNavigation.INFO -> setActiveNavButton(infoButton)
        }
    }

    private fun setActiveNavButton(appCompatButton: AppCompatButton?) {
        if (appCompatButton == null) return
        val index = navButtons.indexOf(appCompatButton)
//        Logger.debug(TAG, "navButtons: $navButtons")
        Logger.debug(TAG, "setActiveNavButton: $index")
        if (index >= 0) {
            activeNavButtonIndex = index
        }
    }

    fun setNavButtonsEnabled() {
        isNavButtonsEnabled = true
    }

    fun setNavButtonsDisabled() {
        isNavButtonsEnabled = false
    }

    private fun setIsNavButtonEnabled(isEnabled: Boolean) {
        navButtons.forEach {
            it.isEnabled = isEnabled
        }
    }

    private fun updateActiveNavButtonTintColor(index: Int) {
        if (index >= 0 && index < navButtons.size) {
            navButtons.forEach {
                setInactiveNavButtonTintColor(it)
            }
            setActiveNavButtonTintColor(navButtons[index])
        }
    }

    private fun setActiveNavButtonTintColor(appCompatButton: AppCompatButton?) {
        setAppCompatButtonColor(appCompatButton, R.color.kenes_blue)
    }

    private fun setInactiveNavButtonTintColor(appCompatButton: AppCompatButton?) {
        setAppCompatButtonColor(appCompatButton, R.color.kenes_gray)
    }

    private fun setAppCompatButtonColor(
        appCompatButton: AppCompatButton?,
        @ColorRes colorResId: Int
    ) {
        if (appCompatButton == null) return

        val compoundDrawable = appCompatButton.getCompoundDrawableOnTop()
        val color = ContextCompat.getColor(context, colorResId)

        if (compoundDrawable != null) {
            val drawableWrap = DrawableCompat.wrap(compoundDrawable).mutate()
            DrawableCompat.setTint(drawableWrap, color)
        }

        appCompatButton.setTextColor(color)
    }

    fun showBottomNavigationView() {
        setBottomNavigationViewVisibility(true)
    }

    fun hideBottomNavigationView() {
        setBottomNavigationViewVisibility(false)
    }

    private fun setBottomNavigationViewVisibility(isVisible: Boolean) {
        bottomNavigationView?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun showNavButton(bottomNavigation: BottomNavigation) {
        Logger.debug(TAG, "showNavButton() -> bottomNavigation: $bottomNavigation")

        val listener = object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                callback?.onNavButtonClicked(bottomNavigation)
            }
        }

        when (bottomNavigation) {
            BottomNavigation.HOME ->
                showNavButton(homeButton, 0, listener)
            BottomNavigation.OPERATOR_CALL -> {
                val index = navButtons.size / 2
                showNavButton(operatorCallButton, index, listener)
            }
            BottomNavigation.SERVICES -> {
                val index = navButtons.size / 2
                showNavButton(servicesButton, index, listener)
            }
            BottomNavigation.INFO -> {
                var index = navButtons.size
                if (index > 0) {
                    index -= 1
                }
                showNavButton(infoButton, index, listener)
            }
        }
    }

    fun hideNavButton(bottomNavigation: BottomNavigation) {
        Logger.debug(TAG, "hideNavButton() -> bottomNavigation: $bottomNavigation")
        when (bottomNavigation) {
            BottomNavigation.HOME -> hideNavButton(homeButton)
            BottomNavigation.OPERATOR_CALL -> hideNavButton(operatorCallButton)
            BottomNavigation.SERVICES -> hideNavButton(servicesButton)
            BottomNavigation.INFO -> hideNavButton(infoButton)
        }
    }

    private fun showNavButton(
        appCompatButton: AppCompatButton?,
        index: Int,
        listener: DebouncedOnClickListener
    ) {
        if (appCompatButton != null) {
            appCompatButton.setOnClickListener(listener)
            if (appCompatButton.visibility != View.VISIBLE) {
                appCompatButton.visibility = View.VISIBLE
            }
            navButtons.add(index, appCompatButton)
        }
    }

    private fun hideNavButton(appCompatButton: AppCompatButton?) {
        if (appCompatButton != null) {
            appCompatButton.setOnClickListener(null)
            if (appCompatButton.visibility != View.GONE) {
                appCompatButton.visibility = View.GONE
            }
            navButtons.remove(appCompatButton)
        }
    }

    interface Callback {
        fun onNavButtonClicked(bottomNavigation: BottomNavigation)
    }

}