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

    init {
        val view = inflate(context, R.layout.kenes_audio_call_view, this)

        audioCallButton = view.findViewById(R.id.audioCallButton)
        audioCallInfoView = view.findViewById(R.id.audioCallInfoView)
    }

    fun setDefaultState(isHideInfoView: Boolean = true) {
        audioCallButton.isEnabled = true
        audioCallInfoView.text = null

        if (isHideInfoView) {
            audioCallInfoView.visibility = View.GONE
        }
    }

    fun setDisabledState(text: String? = null) {
        audioCallButton.isEnabled = false

        if (text.isNullOrBlank()) {
            audioCallInfoView.text = null
        } else {
            audioCallInfoView.text = text
            audioCallInfoView.visibility = View.VISIBLE
        }
    }

    fun setOnCallClickListener(callback: () -> Unit) {
        audioCallButton.setOnClickListener { callback() }
    }

}