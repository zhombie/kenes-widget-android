package q19.kenes_widget.ui.components

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    val localSurfaceView: SurfaceViewRenderer
    val remoteSurfaceView: SurfaceViewRenderer
    private val controlButtonsView: LinearLayout
    private val goToChatButton: AppCompatImageButton
    private val hangupButton: AppCompatImageButton
    private val switchSourceButton: AppCompatImageButton

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_video_dialog, this)

        localSurfaceView = view.findViewById(R.id.localSurfaceView)
        remoteSurfaceView = view.findViewById(R.id.remoteSurfaceView)
        controlButtonsView = view.findViewById(R.id.controlButtonsView)
        goToChatButton = view.findViewById(R.id.goToChatButton)
        hangupButton = view.findViewById(R.id.hangupButton)
        switchSourceButton = view.findViewById(R.id.switchSourceButton)

        remoteSurfaceView.setOnClickListener { callback?.onRemoteFrameClicked() }
        goToChatButton.setOnClickListener { callback?.onGoToChatButtonClicked() }
        hangupButton.setOnClickListener { callback?.onHangupButtonClicked() }
        switchSourceButton.setOnClickListener { callback?.onSwitchSourceButtonClicked() }
    }

    fun setDefaultState() {
        showControlButtons()
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
        controlButtonsView.animate()
            .alpha(if (isVisible) 1.0f else 0.0f)
            .setDuration(150)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    controlButtonsView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationRepeat(animation: Animator?) {}
            })
    }

    fun release() {
        localSurfaceView.release()
        remoteSurfaceView.release()
    }

    interface Callback {
        fun onHangupButtonClicked()
        fun onGoToChatButtonClicked()
        fun onSwitchSourceButtonClicked()
        fun onRemoteFrameClicked()
    }

}