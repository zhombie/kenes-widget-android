package q19.kenes.widget.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

internal fun AppCompatActivity.showSoftKeyboard(view: View) {
    if (view.requestFocus()) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

internal fun AppCompatActivity.hideKeyboard(view: View? = null) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(currentFocus?.windowToken ?: view?.windowToken, 0)
}