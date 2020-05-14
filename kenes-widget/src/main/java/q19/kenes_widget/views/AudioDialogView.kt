package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import com.squareup.picasso.Picasso
import q19.kenes_widget.R
import q19.kenes_widget.util.CircleTransformation

internal class AudioDialogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val avatarView: ImageView
    private val nameView: TextView
    private val goToChatButton: AppCompatImageButton
    private val hangupButton: AppCompatImageButton

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_audio_dialog, this)

        avatarView = view.findViewById(R.id.avatarView)
        nameView = view.findViewById(R.id.nameView)
        goToChatButton = view.findViewById(R.id.goToChatButton)
        hangupButton = view.findViewById(R.id.hangupButton)

        goToChatButton.setOnClickListener {
            callback?.onGoToChatButtonClicked()
        }

        hangupButton.setOnClickListener {
            callback?.onHangUpButtonClicked()
        }
    }

    fun showAvatar(photoUrl: String?) {
        if (!photoUrl.isNullOrBlank()) {
            Picasso.get()
                .load(photoUrl)
                .fit()
                .centerCrop()
                .transform(CircleTransformation())
                .into(avatarView)
        } else {
            avatarView.setImageDrawable(null)
        }
    }

    fun showName(name: String) {
        nameView.text = name
    }

    interface Callback {
        fun onHangUpButtonClicked()
        fun onGoToChatButtonClicked()
    }

}