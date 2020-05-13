package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import org.webrtc.SurfaceViewRenderer
import q19.kenes_widget.R

class VideoDialogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    val localSurfaceView: SurfaceViewRenderer
    val remoteSurfaceView: SurfaceViewRenderer
    val goToChatButton: AppCompatImageButton
    val hangupButton: AppCompatImageButton
    val switchSourceButton: AppCompatImageButton

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_video_dialog, this)

        localSurfaceView = view.findViewById(R.id.localSurfaceView)
        remoteSurfaceView = view.findViewById(R.id.remoteSurfaceView)
        goToChatButton = view.findViewById(R.id.goToChatButton)
        hangupButton = view.findViewById(R.id.hangupButton)
        switchSourceButton = view.findViewById(R.id.switchSourceButton)

        goToChatButton.setOnClickListener {
            callback?.onGoToChatButtonClicked()
        }

        hangupButton.setOnClickListener {
            callback?.onHangUpButtonClicked()
        }

        switchSourceButton.setOnClickListener {
            callback?.onSwitchSourceButtonClicked()
        }
    }

    fun release() {
        localSurfaceView.release()
        remoteSurfaceView.release()
    }

    fun destroy() {
        goToChatButton.hasOnClickListeners()
    }

    interface Callback {
        fun onHangUpButtonClicked()
        fun onGoToChatButtonClicked()
        fun onSwitchSourceButtonClicked()
    }

}