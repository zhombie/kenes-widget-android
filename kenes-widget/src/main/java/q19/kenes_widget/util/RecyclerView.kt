package q19.kenes_widget.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EdgeEffect
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

internal fun ViewGroup.inflate(@LayoutRes layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

internal fun RecyclerView.setOverscrollColor(@ColorRes colorResId: Int) {
    edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            return EdgeEffect(view.context).apply {
                color = ContextCompat.getColor(context, colorResId)
            }
        }
    }
}

internal fun RecyclerView.disableChangeAnimations() {
//    (itemAnimator as? SimpleItemAnimator?)?.supportsChangeAnimations = false
    itemAnimator = null
}