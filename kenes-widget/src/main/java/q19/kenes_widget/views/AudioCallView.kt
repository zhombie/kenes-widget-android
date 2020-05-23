package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import q19.kenes_widget.R

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

    fun setDefaultState() {
        setEnabledState()

        audioCallInfoView.text = null
        audioCallInfoView.visibility = View.GONE

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
        audioCallButton.isEnabled = isEnabled
    }

    fun setInfoText(text: String) {
        audioCallInfoView.text = text
        audioCallInfoView.visibility = View.VISIBLE
    }

    fun setPendingQueueCount(count: Int) {
        queueCountView.text = "В очереди ($count)"
        queueCountView.visibility = View.VISIBLE
    }

    fun setOnCallClickListener(callback: () -> Unit) {
        audioCallButton.setOnClickListener { callback() }
    }

}