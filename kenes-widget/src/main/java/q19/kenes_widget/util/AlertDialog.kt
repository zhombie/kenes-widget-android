package q19.kenes_widget.util

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import q19.kenes_widget.R

internal val Context.AlertDialogBuilder
    get() = AlertDialog.Builder(this, R.style.AlertDialogTheme)


internal fun Context.showHangupConfirmAlert(callback: () -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_attention)
        .setMessage(R.string.kenes_end_dialog)
        .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
            callback()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.kenes_no) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

internal fun Context.showLanguageSelectionAlert(items: Array<String>, callback: (which: Int) -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_select_language_from_list)
        .setSingleChoiceItems(items, -1) { dialog, which ->
            callback(which)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.kenes_cancel) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

internal fun Context.showOpenLinkConfirmAlert(message: String, callback: () -> Unit): AlertDialog? {
    val messageView = FrameLayout(this)

    val textView = TextView(this)

    textView.setTextColor(ContextCompat.getColor(this, R.color.kenes_black))

    val colorStateList = ColorStateListBuilder()
        .addState(IntArray(1) { android.R.attr.state_pressed }, ContextCompat.getColor(this, R.color.kenes_light_blue))
        .addState(IntArray(1) { android.R.attr.state_selected }, ContextCompat.getColor(this, R.color.kenes_light_blue))
        .addState(intArrayOf(), ContextCompat.getColor(this, R.color.kenes_blue))
        .build()

    textView.highlightColor = Color.TRANSPARENT

    textView.setLinkTextColor(colorStateList)

    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

    textView.linksClickable = true

    textView.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        val margin = resources.getDimensionPixelOffset(R.dimen.kenes_dialog_spacing)
        setMargins(margin, margin, margin, margin)
    }

    val spannable = SpannableString(getString(R.string.kenes_open_link_confirm, message))
    textView.autoLinkMask = Linkify.WEB_URLS
    textView.text = spannable
    textView.movementMethod = LinkMovementMethod.getInstance()

    messageView.addView(textView)

    return AlertDialogBuilder
        .setTitle(R.string.kenes_open_link)
        .setView(messageView)
        .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
            callback()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.kenes_no) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

internal fun Context.showPermanentlyDeniedDialog(message: String, callback: (isPositive: Boolean) -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_attention)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton(R.string.kenes_to_settings) { dialog, _ ->
            dialog.dismiss()
            callback(true)
        }
        .setNegativeButton(R.string.kenes_cancel) { dialog, _ ->
            dialog.dismiss()
            callback(false)
        }
        .show()
}

internal fun Context.showWidgetCloseConfirmDialog(callback: () -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_exit_widget_title)
        .setMessage(R.string.kenes_exit_widget_text)
        .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
            dialog.dismiss()
            callback()
        }
        .setNegativeButton(R.string.kenes_no) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

internal fun Context.showNoOnlineCallAgents(message: String?, callback: () -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_attention)
        .setMessage(message)
        .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
            dialog.dismiss()
            callback()
        }
        .setOnCancelListener {
            callback()
        }
        .setOnDismissListener {
            callback()
        }
        .show()
}

internal fun Context.showAlreadyCallingAlert(callback: () -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_attention)
        .setMessage(R.string.kenes_already_calling_to_agent)
        .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
            dialog.dismiss()
        }
        .setNegativeButton(R.string.kenes_cancel_call) { dialog, _ ->
            dialog.dismiss()
            callback()
        }
        .show()
}

internal fun Context.showFormSentSuccess(callback: () -> Unit): AlertDialog? {
    return AlertDialogBuilder
        .setTitle(R.string.kenes_attention)
        .setMessage(R.string.kenes_form_sent_success)
        .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
            dialog.dismiss()
            callback()
        }
        .show()
}