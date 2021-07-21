package q19.kenes.widget.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import q19.kenes_widget.R

internal class MessageInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val inputView: AppCompatEditText
    private val attachmentButton: AppCompatImageButton
    private val sendMessageButton: AppCompatImageButton

    var isAttachmentButtonEnabled: Boolean = false
        private set

    private var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.view_message_input, this)

        inputView = view.findViewById(R.id.inputView)
        attachmentButton = view.findViewById(R.id.attachmentButton)
        sendMessageButton = view.findViewById(R.id.sendMessageButton)

        attachmentButton.setOnClickListener { callback?.onNewMediaSelection() }

        sendMessageButton.setOnClickListener {
            callback?.onSendTextMessage(inputView?.text?.toString())
        }
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    fun clearInputViewText() {
        inputView.text?.clear()
    }

    fun enableSendMessageButton() {
        setSendMessageButtonEnabled(true)
    }

    fun disableSendMessageButton() {
        setSendMessageButtonEnabled(false)
    }

    private fun setSendMessageButtonEnabled(isEnabled: Boolean) {
        sendMessageButton.isEnabled = isEnabled
    }

    fun enableAttachmentButton() {
        setAttachmentButtonEnabled(true)
    }

    fun disableAttachmentButton() {
        setAttachmentButtonEnabled(false)
    }

    private fun setAttachmentButtonEnabled(isEnabled: Boolean) {
        isAttachmentButtonEnabled = isEnabled
    }

    fun setOnInputViewFocusChangeListener(
        callback: (v: TextView?, actionId: Int, event: KeyEvent?) -> Boolean
    ) {
        inputView.setOnEditorActionListener { v, actionId, event ->
            callback(v, actionId, event)
        }
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

    interface Callback {
        fun onNewMediaSelection()
        fun onSendTextMessage(message: String?)
    }

}