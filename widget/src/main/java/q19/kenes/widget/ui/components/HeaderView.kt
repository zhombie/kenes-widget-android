package q19.kenes.widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageButton
import kz.q19.domain.model.configs.Configs
import kz.q19.utils.view.isVisible
import q19.kenes.widget.util.loadCircleImage
import q19.kenes_widget.R

internal class HeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val opponentAvatarView: ImageView
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

        hangupButton.setOnClickListener { callback?.onHangupButtonClicked() }
    }

    fun setDefaultState() {
        setOpponentInfo(null)
        hideHangupButton()
    }

    fun setOpponentInfo(opponent: Configs.CallAgent?) {
//        if (opponent?.isDrawableResAvailable == true) {
//            setOpponentAvatar(opponent.drawableRes)
//        } else {
//            setOpponentAvatar(opponent?.avatarUrl)
//        }
//
//        if (!opponent?.name.isNullOrBlank()) {
//            setOpponentName(opponent?.name)
//            opponentNameView.visibility = View.VISIBLE
//        } else {
//            opponentNameView.visibility = View.GONE
//        }
//
//        if (!opponent?.secondName.isNullOrBlank()) {
//            setOpponentSecondName(opponent?.secondName)
//            opponentSecondNameView.visibility = View.VISIBLE
//        } else {
//            opponentSecondNameView.visibility = View.GONE
//        }
    }

    fun setOpponentAvatar(photoUrl: String?) {
        if (!photoUrl.isNullOrBlank()) {
            opponentAvatarView.loadCircleImage(photoUrl)
        } else {
            opponentAvatarView.setImageResource(R.drawable.kenes_placeholder_circle_gray)
        }
    }

    fun setOpponentAvatar(@DrawableRes drawableRes: Int) {
        opponentAvatarView.setImageResource(drawableRes)
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
        hangupButton.isVisible = isVisible
    }

    interface Callback {
        fun onHangupButtonClicked()
    }

}