package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import q19.kenes_widget.R
import q19.kenes_widget.model.OperatorCall

internal class OperatorCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val audioCallButton: LinearLayout
    private val videoCallButton: LinearLayout

    init {
        val view = inflate(context, R.layout.kenes_view_operator_call, this)

        audioCallButton = view.findViewById(R.id.audioCallButton)
        videoCallButton = view.findViewById(R.id.videoCallButton)
    }

    fun showCallButton(operatorCall: OperatorCall) {
        setCallButtonVisibility(operatorCall, true)
    }

    fun hideCallButton(operatorCall: OperatorCall) {
        setCallButtonVisibility(operatorCall, false)
    }

    private fun setCallButtonVisibility(operatorCall: OperatorCall, isVisible: Boolean) {
        if (operatorCall == OperatorCall.AUDIO) {
            audioCallButton.visibility = if (isVisible) View.VISIBLE else View.GONE
        } else if (operatorCall == OperatorCall.VIDEO) {
            videoCallButton.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    fun setCallButtonEnabled(operatorCall: OperatorCall) {
        setCallButtonEnabled(operatorCall, true)
    }

    fun setCallButtonDisabled(operatorCall: OperatorCall) {
        setCallButtonEnabled(operatorCall, false)
    }

    private fun setCallButtonEnabled(operatorCall: OperatorCall, isEnabled: Boolean) {
        if (operatorCall == OperatorCall.AUDIO) {
            if (audioCallButton.isEnabled == isEnabled) return
            audioCallButton.isEnabled = isEnabled
        } else if (operatorCall == OperatorCall.VIDEO) {
            if (videoCallButton.isEnabled == isEnabled) return
            videoCallButton.isEnabled = isEnabled
        }
    }

    fun setOnAudioCallClickListener(
        operatorCall: OperatorCall,
        callback: (operatorCall: OperatorCall) -> Unit
    ) {
        if (operatorCall == OperatorCall.AUDIO) {
            audioCallButton.setOnClickListener { callback(operatorCall) }
        } else if (operatorCall == OperatorCall.VIDEO) {
            videoCallButton.setOnClickListener { callback(operatorCall) }
        }
    }

    fun removeListener(operatorCall: OperatorCall) {
        if (operatorCall == OperatorCall.AUDIO) {
            audioCallButton.setOnClickListener(null)
        } else if (operatorCall == OperatorCall.VIDEO) {
            videoCallButton.setOnClickListener(null)
        }
    }

}