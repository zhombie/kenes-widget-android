package q19.kenes.widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = ProgressView::class.java.simpleName
    }

    private val rootView: LinearLayout
    private val centerView: FrameLayout
    private lateinit var progressIndicator: CircularProgressIndicator
    private val cancelButton: MaterialButton

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(Type.INDETERMINATE, Type.DETERMINATE)
    annotation class Type {
        companion object {
            const val INDETERMINATE = 0
            const val DETERMINATE = 1
        }
    }

    private var isCancelable: Boolean = false
        set(value) {
            field = value
            if (value) {
                if (cancelButton.visibility != View.VISIBLE) {
                    cancelButton.visibility = View.VISIBLE
                }
            } else {
                if (cancelButton.visibility != View.GONE) {
                    cancelButton.visibility = View.GONE
                }
            }
        }

    init {
        val view = inflate(context, R.layout.view_progress, this)

        rootView = view.findViewById(R.id.rootView)
        centerView = view.findViewById(R.id.centerView)
        cancelButton = view.findViewById(R.id.cancelButton)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressView)

        try {
            val backgroundColor = typedArray.getColor(
                R.styleable.ProgressView_backgroundColor,
                ContextCompat.getColor(context, R.color.kenes_black_with_opacity_BB)
            )

            rootView.setBackgroundColor(backgroundColor)

            progressIndicator =
                createProgressIndicator(R.style.Widget_MaterialComponents_CircularProgressIndicator)

            when (typedArray.getInt(R.styleable.ProgressView_type, Type.INDETERMINATE)) {
                Type.INDETERMINATE -> setIndeterminate()
                Type.DETERMINATE -> setDeterminate()
            }

            centerView.addView(progressIndicator)

            isCancelable = typedArray.getBoolean(R.styleable.ProgressView_isCancelable, false)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }

        hide()
    }

    fun show() {
        visibility = View.VISIBLE
        alpha = 0F
        animate()
            .alpha(1F)
            .setDuration(200L)
            .start()
    }

    fun hide() {
        visibility = View.VISIBLE
        alpha = 1F
        animate()
            .alpha(0F)
            .withEndAction { visibility = View.GONE }
            .setDuration(200L)
            .start()
    }

    fun isProgressShown(): Boolean = visibility == View.VISIBLE

    fun isProgressHidden(): Boolean = visibility == View.GONE

    fun setCancelable(isCancelable: Boolean): Boolean {
        this.isCancelable = isCancelable
        return this.isCancelable == isCancelable
    }

    fun setOnCancelClickListener(block: () -> Unit) {
        if (isCancelable) {
            if (!cancelButton.hasOnClickListeners()) {
                cancelButton.setOnClickListener { block() }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun createProgressIndicator(style: Int): CircularProgressIndicator {
        val progressIndicator = CircularProgressIndicator(context, null, style)

        progressIndicator.layoutParams = MarginLayoutParams(
            context.resources.getDimensionPixelOffset(R.dimen.progress_circle_size),
            context.resources.getDimensionPixelOffset(R.dimen.progress_circle_size)
        )

        progressIndicator.indicatorSize = context.resources.getDimensionPixelOffset(R.dimen.progress_circle_size)
        progressIndicator.trackThickness = context.resources.getDimensionPixelOffset(R.dimen.progress_track_thickness)

        progressIndicator.setIndicatorColor(ContextCompat.getColor(context, R.color.kenes_white))

        progressIndicator.trackColor = ContextCompat.getColor(context, R.color.kenes_transparent)

        return progressIndicator
    }

    fun setDeterminate() {
        progressIndicator.isIndeterminate = false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            progressIndicator.min = 0
        }

        progressIndicator.max = 100
    }

    fun setIndeterminate() {
        progressIndicator.isIndeterminate = true
    }

    fun setProgress(progress: Double) {
        progressIndicator.progress = progress.roundToInt()
    }

}