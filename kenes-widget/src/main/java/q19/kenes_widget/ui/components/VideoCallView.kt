package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import q19.kenes_widget.R

internal class VideoCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val videoCallButton: AppCompatButton
    private val cancelCallButton: AppCompatButton
    private val videoCallInfoView: TextView
    private val queueCountView: TextView

    init {
        val view = inflate(context, R.layout.kenes_view_video_call, this)

        videoCallButton = view.findViewById(R.id.videoCallButton)
        cancelCallButton = view.findViewById(R.id.cancelCallButton)
        videoCallInfoView = view.findViewById(R.id.videoCallInfoView)
        queueCountView = view.findViewById(R.id.queueCountView)
    }

    fun setCallButtonEnabled() {
        setCallButtonEnabled(true)
    }

    fun setCallButtonDisabled() {
        setCallButtonEnabled(false)
    }

    private fun setCallButtonEnabled(isEnabled: Boolean) {
        if (videoCallButton.isEnabled == isEnabled) return
        videoCallButton.isEnabled = isEnabled
    }

    fun showCancelCallButton() {
        setCancelCallButtonVisibility(true)
    }

    fun hideCancelCallButton() {
        setCancelCallButtonVisibility(false)
    }

    private fun setCancelCallButtonVisibility(isVisible: Boolean) {
        cancelCallButton.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    fun setInfoViewText(text: String?) {
        videoCallInfoView.text = text
    }

    fun showInfoViewText() {
        if (videoCallInfoView.visibility == View.VISIBLE) return
        videoCallInfoView.visibility = View.VISIBLE
    }

    fun hideInfoViewText() {
        if (videoCallInfoView.visibility == View.GONE) return
        videoCallInfoView.visibility = View.GONE
    }

    fun setPendingQueueCountViewText(text: String?) {
        queueCountView.text = text
    }

    fun showPendingQueueCountView() {
        if (queueCountView.visibility == View.VISIBLE) return
        queueCountView.visibility = View.VISIBLE
    }

    fun hidePendingQueueCountView() {
        if (queueCountView.visibility == View.GONE) return
        queueCountView.visibility = View.GONE
    }

    fun setOnCallClickListener(callback: () -> Unit) {
        videoCallButton.setOnClickListener { callback() }
    }

    fun setOnCancelCallClickListener(callback: () -> Unit) {
        cancelCallButton.setOnClickListener { callback() }
    }

}