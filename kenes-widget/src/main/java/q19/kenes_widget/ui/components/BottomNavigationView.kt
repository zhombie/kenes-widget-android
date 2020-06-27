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

        navButtons.clear()

        homeButton?.setOnClickListener(object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                if (callback?.onHomeNavButtonClicked() == true) {
                    setHomeNavButtonActive()
                }
            }
        })

        homeButton?.let { navButtons.add(0, it) }

        infoButton?.setOnClickListener(object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                if (callback?.onInfoNavButtonClicked() == true) {
                    setInfoNavButtonActive()
                }
            }
        })

        infoButton?.let { navButtons.add(navButtons.size - 1, it) }
    }

    fun setHomeNavButtonActive() {
        setActiveNavButton(homeButton)
    }

    fun setVideoNavButtonActive() {
        setActiveNavButton(videoButton)
    }

    fun setAudioNavButtonActive() {
        setActiveNavButton(audioButton)
    }

    fun setContactsNavButtonActive() {
        setActiveNavButton(contactsButton)
    }

    fun setInfoNavButtonActive() {
        setActiveNavButton(infoButton)
    }

    private fun setActiveNavButton(appCompatImageButton: AppCompatImageButton?) {
        if (appCompatImageButton == null) return
        val index = navButtons.indexOf(appCompatImageButton)
        Logger.debug(TAG, "navButtons: $navButtons")
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

    fun showVideoCallNavButton() {
        val half = navButtons.size / 2
        Logger.debug(TAG, "showVideoCallNavButton -> half: $half")
        showNavButton(videoButton, half, object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                if (callback?.onVideoNavButtonClicked() == true) {
                    setVideoNavButtonActive()
                }
            }
        })
    }

    fun hideVideoCallNavButton() {
        hideNavButton(videoButton)
    }

    fun showAudioCallNavButton() {
        val half = navButtons.size / 2
        Logger.debug(TAG, "showAudioCallNavButton -> half: $half")
        showNavButton(audioButton, half, object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                if (callback?.onAudioNavButtonClicked() == true) {
                    setAudioNavButtonActive()
                }
            }
        })
    }

    fun hideAudioCallNavButton() {
        hideNavButton(audioButton)
    }

    fun showContactsNavButton() {
        val half = navButtons.size / 2
        Logger.debug(TAG, "showContactsNavButton -> half: $half")
        showNavButton(contactsButton, half, object : DebouncedOnClickListener() {
            override fun onDebouncedClick(v: View) {
                if (callback?.onContactsNavButtonClicked() == true) {
                    setContactsNavButtonActive()
                }
            }
        })
    }

    fun hideContactsNavButton() {
        hideNavButton(contactsButton)
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
        fun onHomeNavButtonClicked(): Boolean
        fun onVideoNavButtonClicked(): Boolean
        fun onAudioNavButtonClicked(): Boolean
        fun onContactsNavButtonClicked(): Boolean
        fun onInfoNavButtonClicked(): Boolean
    }

}