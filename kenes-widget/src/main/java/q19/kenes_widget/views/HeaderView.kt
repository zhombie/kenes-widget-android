package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import com.squareup.picasso.Picasso
import q19.kenes_widget.R
import q19.kenes_widget.model.Configs
import q19.kenes_widget.util.CircleTransformation

internal class HeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val opponentAvatarView: AppCompatImageView
    private val opponentNameView: TextView
    private val opponentSecondNameView: TextView
    private val hangupButton: AppCompatImageButton

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_header, this)

        opponentAvatarView = view.findViewById(R.id.opponentAvatarView)
        opponentNameView = view.findViewById(R.id.opponentNameView)
        opponentSecondNameView = view.findViewById(R.id.opponentSecondNameView)
        hangupButton = view.findViewById(R.id.hangupButton)

        hangupButton.setOnClickListener { callback?.onHangUpButtonClicked() }
    }

    fun setDefaultState() {
        setOpponentInfo(null)
        hideHangupButton()
    }

    fun setOpponentInfo(opponent: Configs.Opponent?) {
        setOpponentAvatar(opponent?.avatarUrl)
        setOpponentName(opponent?.name)
        setOpponentSecondName(opponent?.secondName)
    }

    fun setOpponentAvatar(photoUrl: String?) {
        if (!photoUrl.isNullOrBlank()) {
            Picasso.get()
                .load(photoUrl)
                .fit()
                .centerCrop()
                .transform(CircleTransformation())
                .into(opponentAvatarView)
        } else {
            opponentAvatarView.setImageDrawable(null)
        }
    }

    fun setOpponentName(name: String?) {
        opponentNameView.text = name
    }

    fun setOpponentSecondName(secondName: String?) {
        opponentSecondNameView.text = secondName
    }

    fun showHangupButton() {
        setHangupButtonVisibility(true)
    }

    fun hideHangupButton() {
        setHangupButtonVisibility(false)
    }

    private fun setHangupButtonVisibility(isVisible: Boolean) {
        hangupButton.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    interface Callback {
        fun onHangUpButtonClicked()
    }

}