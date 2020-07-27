package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import q19.kenes_widget.R

internal class OperatorCallPendingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val callTypeView: TextView
    private val progressBar: ProgressBar
    private val infoTextView: TextView
//    private val queueCountView: TextView
    private val cancelCallButton: AppCompatImageButton

    init {
        val view = inflate(context, R.layout.kenes_view_operator_pending_call, this)

        callTypeView = view.findViewById(R.id.callTypeView)
        progressBar = view.findViewById(R.id.progressBar)
        infoTextView = view.findViewById(R.id.infoTextView)
//        queueCountView = TextView(context)
        cancelCallButton = view.findViewById(R.id.cancelCallButton)
    }

    fun setCallTypeViewText(text: String?) {
        callTypeView.text = text
    }

    fun showProgress() {
        if (progressBar.visibility == View.VISIBLE) return
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgress() {
        if (progressBar.visibility == View.GONE) return
        progressBar.visibility = View.GONE
    }

    fun setInfoViewText(text: String?) {
        hideProgress()
        infoTextView.text = text
    }

    fun showInfoViewText() {
        if (infoTextView.visibility == View.VISIBLE) return
        infoTextView.visibility = View.VISIBLE
    }

    fun hideInfoViewText() {
        if (infoTextView.visibility == View.GONE) return
        infoTextView.visibility = View.GONE
    }

    fun setPendingQueueCountViewText(text: String?) {
//        queueCountView.text = text
    }

    fun showPendingQueueCountView() {
//        if (queueCountView.visibility == View.VISIBLE) return
//        queueCountView.visibility = View.VISIBLE
    }

    fun hidePendingQueueCountView() {
//        if (queueCountView.visibility == View.GONE) return
//        queueCountView.visibility = View.GONE
    }

    fun setCancelCallButtonEnabled() {
        setCancelCallButtonEnabled(true)
    }

    fun setCancelCallButtonDisabled() {
        setCancelCallButtonEnabled(false)
    }

    private fun setCancelCallButtonEnabled(isEnabled: Boolean) {
        if (cancelCallButton.isEnabled == isEnabled) return
        cancelCallButton.isEnabled = isEnabled
    }

    fun setOnCancelCallButtonClickListener(callback: () -> Unit) {
        cancelCallButton.setOnClickListener { callback() }
    }

}