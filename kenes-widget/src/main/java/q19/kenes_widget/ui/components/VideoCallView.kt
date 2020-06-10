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
    private val videoCallInfoView: TextView
    private val queueCountView: TextView

    init {
        val view = inflate(context, R.layout.kenes_view_video_call, this)

        videoCallButton = view.findViewById(R.id.videoCallButton)
        videoCallInfoView = view.findViewById(R.id.videoCallInfoView)
        queueCountView = view.findViewById(R.id.queueCountView)
    }

    fun setDefaultState() {
        setEnabledState()

        videoCallInfoView.text = null
        videoCallInfoView.visibility = View.GONE

        queueCountView.text = null
        queueCountView.visibility = View.GONE
    }

    fun setEnabledState() {
        setState(true)
    }

    fun setDisabledState() {
        setState(false)
    }

    private fun setState(isEnabled: Boolean) {
        if (videoCallButton.isEnabled == isEnabled) return
        videoCallButton.isEnabled = isEnabled
    }

    fun setInfoText(text: String) {
        videoCallInfoView.text = text
        videoCallInfoView.visibility = View.VISIBLE
    }

    fun hideInfoText() {
        videoCallInfoView.text = null
        videoCallInfoView.visibility = View.GONE
    }

    fun setPendingQueueCount(count: Int) {
        queueCountView.text = "В очереди $count"
        queueCountView.visibility = View.VISIBLE
    }

    fun hidePendingQueueCount() {
        queueCountView.text = null
        queueCountView.visibility = View.GONE
    }

    fun setOnCallClickListener(callback: () -> Unit) {
        videoCallButton.setOnClickListener { callback() }
    }

}