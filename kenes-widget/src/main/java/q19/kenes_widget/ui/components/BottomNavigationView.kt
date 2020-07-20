package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import q19.kenes_widget.R
import q19.kenes_widget.model.BottomNavigation
import q19.kenes_widget.util.DebouncedOnClickListener
import q19.kenes_widget.util.Logger

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
    private var homeButton: AppCompatImageButton? = null
    private var videoButton: AppCompatImageButton? = null
    private var audioButton: AppCompatImageButton? = null
    private var contactsButton: AppCompatImageButton? = null
    private var infoButton: AppCompatImageButton? = null

    private val navButtons = mutableListOf<AppCompatImageButton>()

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
        videoButton = view.findViewById(R.id.videoButton)
        audioButton = view.findViewById(R.id.audioButton)
        contactsButton = view.findViewById(R.id.contactsButton)
        infoButton = view.findViewById(R.id.infoButton)
    }

    fun getFirstNavButton(): BottomNavigation? {
        return when {
            isNavButtonFirst(homeButton) -> BottomNavigation.HOME
            isNavButtonFirst(videoButton) -> BottomNavigation.VIDEO
            isNavButtonFirst(audioButton) -> BottomNavigation.AUDIO
            isNavButtonFirst(contactsButton) -> BottomNavigation.CONTACTS
            isNavButtonFirst(infoButton) -> BottomNavigation.INFO
            else -> null
        }
    }

    private fun isNavButtonFirst(appCompatImageButton: AppCompatImageButton?): Boolean {
        return navButtons.isNotEmpty() && navButtons.first() == appCompatImageButton
    }

    fun setNavButtonActive(bottomNavigation: BottomNavigation) {
        when (bottomNavigation) {
            BottomNavigation.HOME -> setActiveNavButton(homeButton)
            BottomNavigation.VIDEO -> setActiveNavButton(videoButton)
            BottomNavigation.AUDIO -> setActiveNavButton(audioButton)
            BottomNavigation.CONTACTS -> setActiveNavButton(contactsButton)
            BottomNavigation.INFO -> setActiveNavButton(infoButton)
        }
    }

    private fun setActiveNavButton(appCompatImageButton: AppCompatImageButton?) {
        if (appCompatImageButton == null) return
        val index = navButtons.indexOf(appCompatImageButton)
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

    private fun setActiveNavButtonTintColor(appCompatImageButton: AppCompatImageButton?) {
        setAppCompatImageButtonColorFilter(appCompatImageButton, R.color.kenes_blue)
    }

    private fun setInactiveNavButtonTintColor(appCompatImageButton: AppCompatImageButton?) {
        setAppCompatImageButtonColorFilter(appCompatImageButton, R.color.kenes_gray)
    }

    private fun setAppCompatImageButtonColorFilter(
        appCompatImageButton: AppCompatImageButton?,
        @ColorRes colorResId: Int
    ) {
        appCompatImageButton?.setColorFilter(ContextCompat.getColor(context, colorResId))
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
            BottomNavigation.VIDEO -> {
                val index = navButtons.size / 2
                showNavButton(videoButton, index, listener)
            }
            BottomNavigation.AUDIO -> {
                val index = navButtons.size / 2
                showNavButton(audioButton, index, listener)
            }
            BottomNavigation.CONTACTS -> {
                val index = navButtons.size / 2
                showNavButton(contactsButton, index, listener)
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
            BottomNavigation.VIDEO -> hideNavButton(videoButton)
            BottomNavigation.AUDIO -> hideNavButton(audioButton)
            BottomNavigation.CONTACTS -> hideNavButton(contactsButton)
            BottomNavigation.INFO -> hideNavButton(infoButton)
        }
    }

    private fun showNavButton(
        appCompatImageButton: AppCompatImageButton?,
        index: Int,
        listener: DebouncedOnClickListener
    ) {
        if (appCompatImageButton != null) {
            appCompatImageButton.setOnClickListener(listener)
            appCompatImageButton.visibility = View.VISIBLE
            navButtons.add(index, appCompatImageButton)
        }
    }

    private fun hideNavButton(appCompatImageButton: AppCompatImageButton?) {
        if (appCompatImageButton != null) {
            appCompatImageButton.setOnClickListener(null)
            appCompatImageButton.visibility = View.GONE
            navButtons.remove(appCompatImageButton)
        }
    }

    interface Callback {
        fun onNavButtonClicked(bottomNavigation: BottomNavigation)
    }

}