package q19.kenes_widget.util

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import q19.kenes_widget.R

val Context.AlertDialogBuilder
    get() = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
