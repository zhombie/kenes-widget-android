package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import q19.kenes_widget.R

internal class FooterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val goToActiveDialogButton: AppCompatButton
    private val inputView: AppCompatEditText
    private val attachmentButton: AppCompatImageButton

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_footer, this)

        goToActiveDialogButton = view.findViewById(R.id.goToActiveDialogButton)
        inputView = view.findViewById(R.id.inputView)
        attachmentButton = view.findViewById(R.id.attachmentButton)

        // TODO: Remove after attachment upload ability realization
        attachmentButton.visibility = View.GONE

        goToActiveDialogButton.setOnClickListener { callback?.onGoToActiveDialogButtonClicked() }
        attachmentButton.setOnClickListener { callback?.onAttachmentButtonClicked() }

        inputView.setOnFocusChangeListener { v, hasFocus ->
            callback?.onInputViewFocusChangeListener(v, hasFocus)
        }

        inputView.setOnClickListener { callback?.onInputViewClicked() }
    }

    fun setDefaultState() {
        setGoToActiveDialogButtonState(null)

        clearInputViewText()
    }

    fun setGoToActiveDialogButtonState(@StringRes stringRes: Int? = null) {
        if (stringRes == null) {
            goToActiveDialogButton.text = null
            goToActiveDialogButton.visibility = View.GONE
        } else {
            goToActiveDialogButton.setText(stringRes)
            goToActiveDialogButton.visibility = View.VISIBLE
        }
    }

    fun clearInputViewText() {
        inputView.text?.clear()
    }

    fun getInputView(): AppCompatEditText {
        return inputView
    }

    fun setOnInputViewFocusChangeListener(
        callback: (v: TextView?, actionId: Int, event: KeyEvent?) -> Boolean
    ) {
        inputView.setOnEditorActionListener { v, actionId, event ->
            callback(v, actionId, event)
        }
    }

    interface Callback {
        fun onGoToActiveDialogButtonClicked()
        fun onAttachmentButtonClicked()
        fun onInputViewFocusChangeListener(v: View, hasFocus: Boolean)
        fun onInputViewClicked()
    }

}