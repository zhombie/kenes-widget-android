package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import q19.kenes_widget.R

internal class BottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var homeNavButton: AppCompatImageButton? = null
    private var videoNavButton: AppCompatImageButton? = null
    private var audioNavButton: AppCompatImageButton? = null
    private var infoNavButton: AppCompatImageButton? = null

    private val navButtons
        get() = listOf(homeNavButton, videoNavButton, audioNavButton, infoNavButton)

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

        homeNavButton = view.findViewById(R.id.homeButton)
        videoNavButton = view.findViewById(R.id.videoButton)
        audioNavButton = view.findViewById(R.id.audioButton)
        infoNavButton = view.findViewById(R.id.infoButton)

        homeNavButton?.setOnClickListener {
            setHomeNavButtonActive()
            callback?.onHomeNavButtonClicked()
        }

        videoNavButton?.setOnClickListener {
            setVideoNavButtonActive()
            callback?.onVideoNavButtonClicked()
        }

        audioNavButton?.setOnClickListener {
            setAudioNavButtonActive()
            callback?.onAudioNavButtonClicked()
        }

        infoNavButton?.setOnClickListener {
            setInfoNavButtonActive()
            callback?.onInfoNavButtonClicked()
        }
    }

    fun setHomeNavButtonActive() {
        setActiveNavButton(0)
    }

    fun setVideoNavButtonActive() {
        setActiveNavButton(1)
    }

    fun setAudioNavButtonActive() {
        setActiveNavButton(2)
    }

    fun setInfoNavButtonActive() {
        setActiveNavButton(3)
    }

    private fun setActiveNavButton(index: Int) {
        activeNavButtonIndex = index
    }

    fun setNavButtonsEnabled() {
        isNavButtonsEnabled = true
    }

    fun setNavButtonsDisabled() {
        isNavButtonsEnabled = false
    }

    private fun setIsNavButtonEnabled(isEnabled: Boolean) {
        navButtons.forEach {
            it?.isEnabled = isEnabled
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
        appCompatImageButton?.setColorFilter(ContextCompat.getColor(context, R.color.kenes_blue))
    }

    private fun setInactiveNavButtonTintColor(appCompatImageButton: AppCompatImageButton?) {
        appCompatImageButton?.setColorFilter(ContextCompat.getColor(context, R.color.kenes_gray))
    }

    interface Callback {
        fun onHomeNavButtonClicked()
        fun onVideoNavButtonClicked()
        fun onAudioNavButtonClicked()
        fun onInfoNavButtonClicked()
    }

}