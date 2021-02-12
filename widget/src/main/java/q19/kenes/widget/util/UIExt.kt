package q19.kenes.widget.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout

internal fun View.addKeyboardInsetListener(callback: (visible: Boolean) -> Unit) {
    doOnLayout {
        // get init state of keyboard
        var wasKeyboardVisible = isKeyboardVisible()

        // callback as soon as the layout is set with whether the keyboard is open or not
        callback(wasKeyboardVisible)

        // whenever there is an inset change on the App, check if the keyboard is visible.
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val isKeyboardVisible = isKeyboardVisible()
            // since the observer is hit quite often, only callback when there is a change.
            if (isKeyboardVisible != wasKeyboardVisible) {
                callback(isKeyboardVisible)
                wasKeyboardVisible = isKeyboardVisible
            }

            insets
        }
    }
}


internal fun View.isKeyboardVisible(): Boolean {
    return ViewCompat.getRootWindowInsets(this)
        ?.isVisible(WindowInsetsCompat.Type.ime())
        ?:
        WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets)
            .isVisible(WindowInsetsCompat.Type.ime())
}