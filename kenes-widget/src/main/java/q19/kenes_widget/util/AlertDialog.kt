package q19.kenes_widget.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
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
    return AlertDialogBuilder
        .setTitle(R.string.kenes_open_link)
        .setMessage(message)
        .setPositiveButton(R.string.kenes_yes) { dialog, _ ->
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

internal fun Context.showUnrealizedErrorAlert(): AlertDialog? {
    return AlertDialogBuilder
        .setMessage("Не реализовано")
        .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
            dialog.dismiss()
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