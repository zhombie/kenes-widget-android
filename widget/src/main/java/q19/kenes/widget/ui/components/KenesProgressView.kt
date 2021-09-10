package q19.kenes.widget.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kz.q19.utils.android.dp2Px
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class KenesProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = KenesProgressView::class.java.simpleName
    }

    private var centerView: FrameLayout? = null
    private var progressIndicator: CircularProgressIndicator? = null
    private var cancelButton: MaterialButton? = null

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
                if (cancelButton?.visibility != View.VISIBLE) {
                    cancelButton?.visibility = View.VISIBLE
                }
            } else {
                if (cancelButton?.visibility != View.GONE) {
                    cancelButton?.visibility = View.GONE
                }
            }
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.KenesProgressView)

        var backgroundColor: Int = ContextCompat.getColor(context, R.color.kenes_black_with_opacity_BB)
        var progressType = Type.INDETERMINATE

        try {
            backgroundColor = typedArray.getColor(
                R.styleable.KenesProgressView_kenesBackgroundColor,
                ContextCompat.getColor(context, R.color.kenes_black_with_opacity_BB)
            )

            progressType = typedArray.getInt(R.styleable.KenesProgressView_kenesType, Type.INDETERMINATE)

            isCancelable = typedArray.getBoolean(R.styleable.KenesProgressView_kenesCancelable, false)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }

        setBackgroundColor(backgroundColor)
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER
        orientation = VERTICAL
        visibility = View.GONE

        centerView = buildCenterView()
        progressIndicator = buildProgressIndicator()
        when (progressType) {
            Type.INDETERMINATE -> setIndeterminate()
            Type.DETERMINATE -> setDeterminate()
        }
        centerView?.addView(progressIndicator)
        addView(centerView)

        cancelButton = buildCancelButton()
        addView(cancelButton)
    }

    private fun buildCenterView(): FrameLayout {
        return FrameLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setBackgroundResource(R.drawable.kenes_bg_progress)
            setPadding(
                20F.dp2Px().roundToInt(),
                20F.dp2Px().roundToInt(),
                20F.dp2Px().roundToInt(),
                20F.dp2Px().roundToInt()
            )
        }
    }

    private fun buildProgressIndicator(): CircularProgressIndicator {
        return CircularProgressIndicator(context).apply {
            layoutParams = MarginLayoutParams(
                context.resources.getDimensionPixelOffset(R.dimen.kenes_progress_circle_size),
                context.resources.getDimensionPixelOffset(R.dimen.kenes_progress_circle_size)
            )

            indicatorSize = context.resources.getDimensionPixelOffset(R.dimen.kenes_progress_circle_size)
            trackThickness = context.resources.getDimensionPixelOffset(R.dimen.kenes_progress_track_thickness)

            setIndicatorColor(ContextCompat.getColor(context, R.color.kenes_white))

            trackColor = ContextCompat.getColor(context, R.color.kenes_transparent)
        }
    }

    private fun buildCancelButton(): MaterialButton {
        return MaterialButton(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 25F.dp2Px().roundToInt(), 0, 0)
            }
            minWidth = 125F.dp2Px().roundToInt()
            setBackgroundColor(Color.parseColor("#70333333"))
            cornerRadius = 10F.dp2Px().roundToInt()
            letterSpacing = 0.01F
            isAllCaps = false
            setText(R.string.kenes_cancel)
            setTextColor(ContextCompat.getColor(context, R.color.kenes_white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            setRippleColorResource(R.color.kenes_white)
        }
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

    fun isVisible(): Boolean = visibility == View.VISIBLE

    fun isHidden(): Boolean = visibility == View.GONE

    fun setCancelable(isCancelable: Boolean): Boolean {
        this.isCancelable = isCancelable
        return this.isCancelable == isCancelable
    }

    fun setOnCancelClickListener(block: () -> Unit) {
        if (isCancelable) {
            if (cancelButton?.hasOnClickListeners() == false) {
                cancelButton?.setOnClickListener { block() }
            }
        }
    }

    fun setDeterminate() {
        progressIndicator?.isIndeterminate = false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            progressIndicator?.min = 0
        }

        progressIndicator?.max = 100
    }

    fun setIndeterminate() {
        progressIndicator?.isIndeterminate = true
    }

    fun setProgress(progress: Float) {
        progressIndicator?.progress = progress.roundToInt()
    }

}