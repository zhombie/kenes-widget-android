package q19.kenes.widget.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kz.q19.utils.android.dp2Px
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class KenesMessageInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = KenesChatMessageTextView::class.java.simpleName
    }

    private var selectAttachmentButton: MaterialButton? = null
    private var inputView: TextInputEditText? = null
    private var sendMessageButton: MaterialButton? = null

    var isAttachmentSelectionEnabled: Boolean = false
        private set

    init {
        elevation = 5F.dp2Px()
        setBackgroundColor(ContextCompat.getColor(context, R.color.kenes_white))
        setPadding(
            7F.dp2Px().roundToInt(),
            10F.dp2Px().roundToInt(),
            7F.dp2Px().roundToInt(),
            10F.dp2Px().roundToInt()
        )

        val linearLayout = LinearLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.kenes_bg_message_input_view)
            setPadding(
                8F.dp2Px().roundToInt(),
                7F.dp2Px().roundToInt(),
                8F.dp2Px().roundToInt(),
                7F.dp2Px().roundToInt()
            )
        }

        selectAttachmentButton = buildSelectAttachmentButton()
        linearLayout.addView(selectAttachmentButton)

        inputView = buildInputView()
        linearLayout.addView(inputView)

        sendMessageButton = buildSendMessageButton()
        linearLayout.addView(sendMessageButton)

        addView(linearLayout)
    }

    override fun onDetachedFromWindow() {
        setOnSelectAttachmentClickListener(null)
        setOnSendMessageClickListener(null)
        super.onDetachedFromWindow()
    }

    private fun buildSelectAttachmentButton(): KenesIconButton {
        return KenesIconButton(context).apply {
            id = generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    0,
                    0,
                    5F.dp2Px().roundToInt(),
                    0
                )
            }
            setPadding(8F.dp2Px().roundToInt())
            setIconResource(R.drawable.kenes_ic_attachment)
            iconSize = 22F.dp2Px().roundToInt()
            iconTint = ColorStateList.valueOf(Color.parseColor("#707579"))
        }
    }

    private fun buildInputView(): TextInputEditText {
        return TextInputEditText(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1F
            ).apply {
                setMargins(10F.dp2Px().roundToInt(), 0, 0, 0)
            }
            background = null
            isCursorVisible = true
            setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            setHint(R.string.kenes_hint_message_input_field)
            setLineSpacing(0F, 3F)
            maxLines = 6
            setPadding(
                0,
                5F.dp2Px().roundToInt(),
                0,
                5F.dp2Px().roundToInt()
            )
            isSingleLine = false
            setTextColor(ContextCompat.getColor(context, R.color.kenes_dark_charcoal))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setTextCursorDrawable(R.drawable.kenes_ic_cursor)
            }

            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        }
    }

    private fun buildSendMessageButton(): MaterialButton {
        return KenesIconButton(context).apply {
            id = generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    5F.dp2Px().roundToInt(),
                    0,
                    0,
                    0
                )
            }
            setPadding(8F.dp2Px().roundToInt())
            setIconResource(R.drawable.kenes_ic_send)
            iconSize = 24F.dp2Px().roundToInt()
            setIconTintResource(R.color.kenes_blue)
        }
    }

    fun setAttachmentButtonVisible(isVisible: Boolean) {
        if (isVisible) {
            selectAttachmentButton?.visibility = View.VISIBLE
        } else {
            selectAttachmentButton?.visibility = View.GONE
        }
    }

    fun setAttachmentSelectionEnabled(isEnabled: Boolean) {
        isAttachmentSelectionEnabled = isEnabled
    }

    fun setSendMessageButtonEnabled(isEnabled: Boolean) {
        sendMessageButton?.isEnabled = isEnabled
    }

    fun clearInputViewText() {
        inputView?.text?.clear()
    }

    fun setOnTextChangedListener(
        callback: (s: CharSequence?, start: Int, before: Int, count: Int) -> Unit
    ) {
        inputView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                callback(s, start, before, count)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun setOnSelectAttachmentClickListener(listener: OnClickListener?) {
        selectAttachmentButton?.setOnClickListener(listener)
    }

    fun setOnSendMessageClickListener(onSendMessage: ((view: View, message: String?) -> Unit)?) {
        if (onSendMessage == null) {
            sendMessageButton?.setOnClickListener(onSendMessage)
        } else {
            sendMessageButton?.setOnClickListener {
                onSendMessage.invoke(it, inputView?.text?.toString())
            }
        }
    }

}