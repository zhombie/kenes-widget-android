package q19.kenes_widget.util

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

internal fun <T> lazyUnsychronized(initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE, initializer)

internal fun <T : View> AppCompatActivity.bind(@IdRes res: Int): Lazy<T> =
    lazyUnsychronized { findViewById(res) as T }

internal fun <T : View> View.bind(@IdRes res: Int): Lazy<T> =
    lazyUnsychronized { findViewById(res) as T }

internal fun <T : View> RecyclerView.ViewHolder.bind(@IdRes idRes: Int): Lazy<T> =
    lazyUnsychronized { itemView.findViewById(idRes) as T }