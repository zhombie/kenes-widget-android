package q19.kenes.widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import org.webrtc.SurfaceViewRenderer
import q19.kenes_widget.R

internal class VideoDialogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val overlayView: FrameLayout
    val localSurfaceView: SurfaceViewRenderer
    val remoteSurfaceView: SurfaceViewRenderer
    private val controlButtonsView: RelativeLayout
    private val goToChatButton: AppCompatImageButton
    private val hangupButton: AppCompatImageButton
    private val switchSourceButton: AppCompatImageButton
    private val switchScalingButton: AppCompatImageButton
    private val unreadMessagesCountView: TextView

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_video_dialog, this)

        overlayView = view.findViewById(R.id.overlayView)
        localSurfaceView = view.findViewById(R.id.localSurfaceView)
        remoteSurfaceView = view.findViewById(R.id.remoteSurfaceView)
        controlButtonsView = view.findViewById(R.id.controlButtonsView)
        goToChatButton = view.findViewById(R.id.goToChatButton)
        hangupButton = view.findViewById(R.id.hangupButton)
        switchSourceButton = view.findViewById(R.id.switchSourceButton)
        switchScalingButton = view.findViewById(R.id.switchScalingButton)
        unreadMessagesCountView = view.findViewById(R.id.unreadMessagesCountView)

        localSurfaceView.setOnClickListener {
            if (!isControlButtonsVisible()) {
                showControlButtons()
            }
        }

        overlayView.setOnClickListener { callback?.onFullscreenScreenClicked() }

        goToChatButton.setOnClickListener { callback?.onGoToChatButtonClicked() }
        hangupButton.setOnClickListener { callback?.onHangupButtonClicked() }
        switchSourceButton.setOnClickListener { callback?.onSwitchSourceButtonClicked() }
        switchScalingButton.setOnClickListener { callback?.onSwitchScalingButtonClicked() }
    }

    fun setDefaultState() {
        showControlButtons()

        hideUnreadMessagesCounter()
    }

    fun isControlButtonsVisible(): Boolean {
        return controlButtonsView.visibility == View.VISIBLE
    }

    fun showControlButtons() {
        setControlButtonsVisibility(true)
    }

    fun hideControlButtons() {
        setControlButtonsVisibility(false)
    }

    private fun setControlButtonsVisibility(isVisible: Boolean) {
        controlButtonsView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    fun showUnreadMessagesCounter() {
        setUnreadMessagesCounterVisibility(true)
    }

    fun hideUnreadMessagesCounter() {
        setUnreadMessagesCounterVisibility(false)
    }

    private fun setUnreadMessagesCounterVisibility(isVisible: Boolean) {
        if (isVisible && unreadMessagesCountView.visibility == View.VISIBLE) return
        if (!isVisible && unreadMessagesCountView.visibility == View.GONE) return
        unreadMessagesCountView.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun isUnreadMessagesCounterVisible(): Boolean {
        return unreadMessagesCountView.visibility == View.VISIBLE
    }

    fun isUnreadMessagesCounterHidden(): Boolean {
        return unreadMessagesCountView.visibility == View.GONE
    }

    fun setUnreadMessagesCount(value: String) {
        unreadMessagesCountView.text = value
    }

    fun setSwitchScaleIcon(isFilled: Boolean) {
        switchScalingButton.isActivated = !isFilled
    }

    fun release() {
        localSurfaceView.release()
        remoteSurfaceView.release()
    }

    interface Callback {
        fun onHangupButtonClicked()
        fun onGoToChatButtonClicked()
        fun onSwitchSourceButtonClicked()
        fun onSwitchScalingButtonClicked()
        fun onFullscreenScreenClicked()
    }

}