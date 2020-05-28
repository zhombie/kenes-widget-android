package q19.kenes_widget.util

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

fun <T> lazyUnsychronized(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun <T : View> Activity.bind(@IdRes res : Int) : Lazy<T> {
    return lazyUnsychronized { findViewById(res) as T }
}

fun <T : View> View.bind(@IdRes res : Int) : Lazy<T> {
    return lazyUnsychronized { findViewById(res) as T }
}

fun <T : View> RecyclerView.ViewHolder.bind(@IdRes idRes: Int): Lazy<T> {
    return lazyUnsychronized { itemView.findViewById(idRes) as T }
}