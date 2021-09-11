package q19.kenes.widget.util

import android.view.animation.Animation

internal abstract class AbstractAnimationListener : Animation.AnimationListener {

    override fun onAnimationStart(animation: Animation?) {}

    override fun onAnimationRepeat(animation: Animation?) {}

    override fun onAnimationEnd(animation: Animation?) {}

}