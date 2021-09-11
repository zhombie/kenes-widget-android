package q19.kenes.widget.util

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout

internal fun Window.addKeyboardVisibilityListener(block: (isVisible: Boolean) -> Unit) {
    decorView.doOnLayout {
        // Store initial state of keyboard
        var wasKeyboardVisible = it.isKeyboardVisible()

        // Callback as soon as the layout is set with whether the keyboard is open or not
        block(wasKeyboardVisible)

        // Whenever there is an inset change on the App, check if the keyboard is visible.
        ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets ->
            val isKeyboardVisible = it.isKeyboardVisible()
            // Since the observer is hit quite often, only callback when there is a change.
            if (isKeyboardVisible != wasKeyboardVisible) {
                block(isKeyboardVisible)
                wasKeyboardVisible = isKeyboardVisible
            }

            insets
        }
    }
}


internal fun View.isKeyboardVisible(): Boolean {
    return ViewCompat.getRootWindowInsets(this)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
}

internal fun View.hideKeyboardCompat() {
    ViewCompat.getWindowInsetsController(this)
        ?.hide(WindowInsetsCompat.Type.ime())
}