package q19.kenes.widget.ui.components.transition

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.transition.Transition
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * A [Transition] which animates the rotation of a [View].
 */
internal class RotateX constructor(
    context: Context,
    attrs: AttributeSet
) : Transition(context, attrs) {

    companion object {
        private const val PROP_ROTATION = "kenes:widget:rotate:rotation"

        private val TRANSITION_PROPERTIES = arrayOf(PROP_ROTATION)
    }

    override fun getTransitionProperties(): Array<String> =
        TRANSITION_PROPERTIES

    override fun captureStartValues(transitionValues: TransitionValues) =
        captureValues(transitionValues)

    override fun captureEndValues(transitionValues: TransitionValues) =
        captureValues(transitionValues)

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null

        val startRotation = startValues.values[PROP_ROTATION] as Float
        val endRotation = endValues.values[PROP_ROTATION] as Float
        if (startRotation == endRotation) return null

        val view = endValues.view
        // ensure the pivot is set
        view.pivotX = view.width / 2F
        view.pivotY = view.height / 2F
        return ObjectAnimator.ofFloat(view, View.ROTATION_X, startRotation, endRotation)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view.width <= 0 || view.height <= 0) return
        transitionValues.values[PROP_ROTATION] = view.rotationX
    }

}