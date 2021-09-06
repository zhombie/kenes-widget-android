package q19.kenes.widget.ui.presentation.common.chat.viewholder

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.KenesChatMessageTextView
import q19.kenes.widget.ui.components.KenesChatMessageTimeView
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.util.formatToDigitalClock
import q19.kenes_widget.R
import java.time.Duration

internal class IncomingAudioMessageViewHolder constructor(
    view: View,
    private val callback: ChatMessagesAdapter.Callback? = null
) : MessageViewHolder(view) {

    companion object {
        private val TAG = IncomingAudioMessageViewHolder::class.java.simpleName

        val LAYOUT = R.layout.cell_incoming_audio_message
    }

    private val indicatorView = view.findViewById<FrameLayout>(R.id.indicatorView)
    private val iconView = view.findViewById<ShapeableImageView>(R.id.iconView)
    private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
    private val slider = view.findViewById<Slider>(R.id.slider)
    private val playTimeView = view.findViewById<MaterialTextView>(R.id.playTimeView)
    private val textView = view.findViewById<KenesChatMessageTextView>(R.id.textView)
    private val timeView = view.findViewById<KenesChatMessageTimeView>(R.id.timeView)

    private var sliderAnimator: ValueAnimator? = null

    override fun bind(message: Message) {
        titleView.text = message.media?.title

        playTimeView.text = formatAudioProgress(0, 0)

        if (message.htmlText.isNullOrBlank()) {
            textView.visibility = View.GONE
        } else {
            textView.setHtmlText(message.htmlText) { _, url ->
                callback?.onUrlInTextClicked(url)
            }

            textView.setOnLongClickListener {
                callback?.onMessageLongClicked(message.htmlText.toString())
                true
            }

            textView.visibility = View.VISIBLE
        }

        timeView.text = message.time

        indicatorView.setOnClickListener {
            message.media?.let { media ->
                callback?.onAudioClicked(media, absoluteAdapterPosition)
            }
        }
    }

    fun setAudioPlaybackState(isPlaying: Boolean) {
        Logger.debug(TAG, "setAudioPlaybackState() -> isPlaying: $isPlaying")

        if (isPlaying) {
            iconView.setImageResource(R.drawable.ic_pause)
        } else {
            iconView.setImageResource(R.drawable.ic_play)
        }
    }

    fun resetAudioPlaybackState(duration: Long) {
        Logger.debug(TAG, "resetAudioPlaybackState() -> duration: $duration")

        sliderAnimator?.removeAllUpdateListeners()
        sliderAnimator?.removeAllListeners()
        sliderAnimator?.cancel()
        sliderAnimator = null

        val valueTo = Duration.ofMillis(duration).seconds.toFloat()

        if (slider.valueFrom != 0F) {
            slider.valueFrom = 0F
        }

        if (slider.valueTo != valueTo) {
            slider.valueTo = valueTo
        }

        val animator = ValueAnimator.ofFloat(valueTo, 0F)
        animator?.duration = 150L
        animator?.interpolator = LinearInterpolator()
        animator?.addUpdateListener {
            slider.value = it.animatedValue as Float
        }
        animator?.start()

        playTimeView.text = formatAudioProgress(0, duration)
    }

    fun setAudioPlayProgress(currentPosition: Long, duration: Long, progress: Float) {
        Logger.debug(TAG, "setAudioPlayProgress() -> progress: $progress")

        val valueTo = Duration.ofMillis(duration).seconds.toFloat()

        if (slider.valueFrom != 0F) {
            slider.valueFrom = 0F
        }

        if (slider.valueTo != valueTo) {
            slider.valueTo = valueTo
        }

        sliderAnimator?.removeAllUpdateListeners()
        sliderAnimator?.removeAllListeners()
        sliderAnimator?.cancel()
        sliderAnimator = null

        sliderAnimator = ValueAnimator.ofFloat(slider.value, valueTo * progress / 100)
        sliderAnimator?.duration = 1000L
        sliderAnimator?.interpolator = LinearInterpolator()
        sliderAnimator?.addUpdateListener {
            slider.value = it.animatedValue as Float
        }
        sliderAnimator?.start()

        playTimeView.text = formatAudioProgress(currentPosition, duration)
    }

    private fun formatAudioProgress(current: Long, duration: Long): String {
        return current.formatToDigitalClock() + " / " + duration.formatToDigitalClock()
    }

}