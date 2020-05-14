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

internal class VideoCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val videoCallButton: AppCompatButton
    private val videoCallInfoView: TextView

    init {
        val view = inflate(context, R.layout.kenes_video_call_view, this)

        videoCallButton = view.findViewById(R.id.videoCallButton)
        videoCallInfoView = view.findViewById(R.id.videoCallInfoView)
    }

    fun setDefaultState(isHideInfoView: Boolean = true) {
        videoCallButton.isEnabled = true
        videoCallInfoView.text = null

        if (isHideInfoView) {
            videoCallInfoView.visibility = View.GONE
        }
    }

    fun setDisabledState(text: String? = null) {
        videoCallButton.isEnabled = false

        if (text.isNullOrBlank()) {
            videoCallInfoView.text = null
        } else {
            videoCallInfoView.text = text
            videoCallInfoView.visibility = View.VISIBLE
        }
    }

    fun setOnCallClickListener(callback: () -> Unit) {
        videoCallButton.setOnClickListener { callback() }
    }

}