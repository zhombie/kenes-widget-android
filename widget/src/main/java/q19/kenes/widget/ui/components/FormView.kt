package q19.kenes.widget.ui.components

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import kz.q19.utils.textview.AbstractTextWatcher
import q19.kenes_widget.R

class FormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val REGEX_EMAIL =
            "(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])".toRegex()
        private val REGEX_PHONE = "^[78]7[0-9]{9}\$".toRegex()
    }

    private var titleView: TextView? = null

    private var nameLabelView: TextView? = null
    private var emailLabelView: TextView? = null
    private var phoneLabelView: TextView? = null

    private var nameEditText: EditText? = null
    private var emailEditText: EditText? = null
    private var phoneEditText: EditText? = null

    private var cancelButton: AppCompatButton? = null
    private var sendButton: AppCompatButton? = null

    var callback: Callback? = null

    private val nameTextWatcher = object : AbstractTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            isNameValid()
        }
    }

    private val emailTextWatcher = object : AbstractTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            isEmailValid()
        }
    }

    private val phoneTextWatcher = object : AbstractTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            isPhoneValid()
        }
    }

    init {
        val view = inflate(context, R.layout.kenes_view_form, this)

        titleView = view.findViewById(R.id.titleView)
        nameLabelView = view.findViewById(R.id.nameLabelView)
        emailLabelView = view.findViewById(R.id.emailLabelView)
        phoneLabelView = view.findViewById(R.id.phoneLabelView)
        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        cancelButton = view.findViewById(R.id.cancelButton)
        sendButton = view.findViewById(R.id.sendButton)

        nameLabelView?.text = buildSpannableString(R.string.kenes_name)
        emailLabelView?.text = buildSpannableString(R.string.kenes_email)
        phoneLabelView?.text = buildSpannableString(R.string.kenes_phone)

        nameEditText?.addTextChangedListener(nameTextWatcher)
        emailEditText?.addTextChangedListener(emailTextWatcher)
        phoneEditText?.addTextChangedListener(phoneTextWatcher)

        cancelButton?.setOnClickListener { callback?.onCancelClicked() }

        sendButton?.setOnClickListener {
            if (!isNameValid() || !isEmailValid() || !isPhoneValid()) return@setOnClickListener

            callback?.onSendClicked(
                getName() ?: return@setOnClickListener,
                getEmail() ?: return@setOnClickListener,
                getPhone() ?: return@setOnClickListener
            )
        }
    }

    private fun buildSpannableString(@StringRes resId: Int): SpannableString {
        val spannable = SpannableString("* " + context.getString(resId))
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.kenes_soft_red)),
            0,
            1,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun getName(): String? {
        return nameEditText?.text?.toString()
    }

    private fun getEmail(): String? {
        return emailEditText?.text?.toString()
    }

    private fun getPhone(): String? {
        return phoneEditText?.text?.toString()
    }

    private fun isNameValid(): Boolean {
        val name = getName()
        return if (name.isNullOrBlank()) {
            setNameEditTextEmptyError()
            false
        } else {
            clearNameEditTextError()
            true
        }
    }

    private fun isEmailValid(): Boolean {
        val email = getEmail()
        return when {
            email.isNullOrBlank() -> {
                setEmailEditTextEmptyError()
                false
            }
            !email.matches(REGEX_EMAIL) -> {
                setEmailEditTextIncorrectError()
                false
            }
            else -> {
                clearEmailEditTextError()
                true
            }
        }
    }

    private fun isPhoneValid(): Boolean {
        val phone = getPhone()
        return when {
            phone.isNullOrBlank() -> {
                setPhoneEditTextEmptyError()
                false
            }
            !phone.matches(REGEX_PHONE) -> {
                setPhoneEditTextIncorrectError()
                false
            }
            else -> {
                clearPhoneEditTextError()
                true
            }
        }
    }

    private fun setNameEditTextEmptyError() {
        nameEditText?.setEmptyTextError(R.string.kenes_enter_name)
        nameEditText?.showUiError()
    }

    private fun setEmailEditTextEmptyError() {
        emailEditText?.setEmptyTextError(R.string.kenes_enter_email)
        emailEditText?.showUiError()
    }

    private fun setPhoneEditTextEmptyError() {
        phoneEditText?.setEmptyTextError(R.string.kenes_enter_phone)
        phoneEditText?.showUiError()
    }

    private fun setEmailEditTextIncorrectError() {
        emailEditText?.setEmptyTextError(R.string.kenes_enter_valid_email)
        emailEditText?.showUiError()
    }

    private fun setPhoneEditTextIncorrectError() {
        phoneEditText?.setEmptyTextError(R.string.kenes_enter_valid_phone)
        phoneEditText?.showUiError()
    }

    private fun clearNameEditTextError() {
        nameEditText?.clearError()
        nameEditText?.hideUiError()
    }

    private fun clearEmailEditTextError() {
        emailEditText?.clearError()
        emailEditText?.hideUiError()
    }

    private fun clearPhoneEditTextError() {
        phoneEditText?.clearError()
        phoneEditText?.hideUiError()
    }

    private fun EditText?.setEmptyTextError(@StringRes resId: Int) {
        this?.error = context.getString(resId)
    }

    private fun EditText?.clearError() {
        this?.error = null
    }

    private fun EditText?.showUiError() {
        this?.isActivated = true
    }

    private fun EditText?.hideUiError() {
        this?.isActivated = false
    }

    fun clearInputViews() {
        nameEditText?.text = null
        emailEditText?.text = null
        phoneEditText?.text = null

        nameEditText?.hideUiError()
        emailEditText?.hideUiError()
        phoneEditText?.hideUiError()
    }

    fun clear() {
        titleView = null

        nameLabelView = null
        emailLabelView = null
        phoneLabelView = null

        cancelButton = null
        sendButton = null

        nameEditText?.removeTextChangedListener(nameTextWatcher)
        nameEditText = null

        emailEditText?.removeTextChangedListener(emailTextWatcher)
        emailEditText = null

        phoneEditText?.removeTextChangedListener(phoneTextWatcher)
        phoneEditText = null

        callback = null
    }

    interface Callback {
        fun onCancelClicked()
        fun onSendClicked(name: String, email: String, phone: String)
    }

}