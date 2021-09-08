package q19.kenes.widget.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import q19.kenes_widget.R

internal class KenesMessageInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val selectAttachmentButton: MaterialButton
    private val inputView: TextInputEditText
    private val sendMessageButton: MaterialButton

    var isAttachmentButtonEnabled: Boolean = false
        private set

    init {
        val view = inflate(context, R.layout.view_message_input, this)

        selectAttachmentButton = view.findViewById(R.id.selectAttachmentButton)
        inputView = view.findViewById(R.id.inputView)
        sendMessageButton = view.findViewById(R.id.sendMessageButton)
    }

    override fun onDetachedFromWindow() {
        setOnSelectAttachmentClickListener(null)
        setOnSendMessageClickListener(null)
        super.onDetachedFromWindow()
    }

    fun setAttachmentButtonVisible(isVisible: Boolean) {
        if (isVisible) {
            selectAttachmentButton.visibility = View.VISIBLE
        } else {
            selectAttachmentButton.visibility = View.GONE
        }
    }

    fun setAttachmentButtonEnabled(isEnabled: Boolean) {
        isAttachmentButtonEnabled = isEnabled
    }

    fun setSendMessageButtonEnabled(isEnabled: Boolean) {
        sendMessageButton.isEnabled = isEnabled
    }

    fun clearInputViewText() {
        inputView.text?.clear()
    }

    fun setOnTextChangedListener(
        callback: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit
    ) {
        inputView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                callback(s, start, before, count)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun setOnSelectAttachmentClickListener(listener: OnClickListener?) {
        selectAttachmentButton.setOnClickListener(listener)
    }

    fun setOnSendMessageClickListener(onSendMessage: ((view: View, message: String?) -> Unit)?) {
        if (onSendMessage == null) {
            sendMessageButton.setOnClickListener(onSendMessage)
        } else {
            sendMessageButton.setOnClickListener {
                onSendMessage.invoke(it, inputView.text?.toString())
            }
        }
    }

}