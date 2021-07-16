package q19.kenes.widget.ui.components.deprecated

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import q19.kenes_widget.R

@Deprecated("Use [OperatorCallView]")
internal class AudioCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val audioCallButton: AppCompatButton
    private val audioCallInfoView: TextView
    private val queueCountView: TextView

    init {
        val view = inflate(context, R.layout.kenes_view_audio_call, this)

        audioCallButton = view.findViewById(R.id.audioCallButton)
        audioCallInfoView = view.findViewById(R.id.audioCallInfoView)
        queueCountView = view.findViewById(R.id.queueCountView)
    }

    fun setCallButtonEnabled() {
        setCallButtonEnabled(true)
    }

    fun setCallButtonDisabled() {
        setCallButtonEnabled(false)
    }

    private fun setCallButtonEnabled(isEnabled: Boolean) {
        if (audioCallButton.isEnabled == isEnabled) return
        audioCallButton.isEnabled = isEnabled
    }

    fun setInfoViewText(text: String?) {
        audioCallInfoView.text = text
    }

    fun showInfoViewText() {
        if (audioCallInfoView.visibility == View.VISIBLE) return
        audioCallInfoView.visibility = View.VISIBLE
    }

    fun hideInfoViewText() {
        if (audioCallInfoView.visibility == View.GONE) return
        audioCallInfoView.visibility = View.GONE
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
        audioCallButton.setOnClickListener { callback() }
    }

}