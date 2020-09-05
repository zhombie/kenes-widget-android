package q19.kenes_widget.util

import android.os.SystemClock
import android.view.View
import java.util.*

/**
 * A Debounced OnClickListener
 * Rejects clicks that are too close together in time.
 * This class is safe to use as an OnClickListener for multiple views, and will debounce each one separately.
 *
 * The one and only constructor
 * [minimumInterval] The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
 */
abstract class DebouncedOnClickListener(private val minimumInterval: Long = 300) : View.OnClickListener {

    private val lastClickMap: MutableMap<View, Long>

    init {
        lastClickMap = WeakHashMap()
    }

    /**
     * Implement this in your subclass instead of onClick
     * @param v The view that was clicked
     */
    abstract fun onDebouncedClick(v: View)

    override fun onClick(v: View?) {
        if (v == null) {
            return
        }

        val previousClickTimestamp = lastClickMap[v] ?: 0L
        val currentTimestamp = SystemClock.uptimeMillis()
        lastClickMap[v] = currentTimestamp
        if (currentTimestamp - previousClickTimestamp > minimumInterval) {
            onDebouncedClick(v)
        }
    }

}