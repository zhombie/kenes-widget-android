package q19.kenes.widget.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textview.MaterialTextView
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.util.loadImage
import q19.kenes.widget.util.picasso.CircleTransformation
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class Toolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var imageView: ShapeableImageView? = null
    private var titleView: MaterialTextView? = null
    private var subtitleView: MaterialTextView? = null

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.kenes_white))
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        setPadding(16F.dp2Px().roundToInt(), 0, 16F.dp2Px().roundToInt(), 0)

        addImageView()
        addLinearLayout()
    }

    private fun addImageView() {
        imageView = ShapeableImageView(context)
        imageView?.id = View.generateViewId()
        imageView?.layoutParams = LayoutParams(45F.dp2Px().roundToInt(), 45F.dp2Px().roundToInt())
        imageView?.setContentPadding(
            5F.dp2Px().roundToInt(),
            5F.dp2Px().roundToInt(),
            5F.dp2Px().roundToInt(),
            5F.dp2Px().roundToInt()
        )
        imageView?.shapeAppearanceModel = ShapeAppearanceModel
            .builder(context, R.style.Kenes_Widget_ShapeAppearance_Circle, 0)
            .build()
        imageView?.strokeColor = ColorStateList.valueOf(Color.parseColor("#1ABDBDBD"))
        imageView?.strokeWidth = 0.5F.dp2Px()
        addView(imageView)
    }

    private fun addLinearLayout() {
        val layout = LinearLayout(context)
        layout.id = View.generateViewId()
        layout.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1F).apply {
            setMargins(10F.dp2Px().roundToInt(), 0, 0, 0)
        }
        layout.orientation = VERTICAL

        titleView = MaterialTextView(context)
        titleView?.id = View.generateViewId()
        titleView?.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        titleView?.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
        titleView?.includeFontPadding = false
        titleView?.letterSpacing = 0F
        titleView?.maxLines = 1
        titleView?.isSingleLine = true
        titleView?.isAllCaps = false
        titleView?.setTextColor(ContextCompat.getColor(context, R.color.kenes_dark_charcoal))
        titleView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
        layout.addView(titleView)

        subtitleView = MaterialTextView(context)
        subtitleView?.id = View.generateViewId()
        subtitleView?.layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 2F.dp2Px().roundToInt(), 0, 0)
        }
        subtitleView?.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
        subtitleView?.includeFontPadding = false
        subtitleView?.letterSpacing = 0F
        subtitleView?.maxLines = 1
        subtitleView?.isSingleLine = true
        subtitleView?.isAllCaps = false
        subtitleView?.setTextColor(Color.parseColor("#8D8D8D"))
        subtitleView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11F)
        layout.addView(subtitleView)

        addView(layout)
    }

    fun showImage(imageUrl: String?) {
        imageView?.loadImage(imageUrl, transformation = CircleTransformation())
    }

    fun setTitle(title: String?) {
        titleView?.text = title
    }

    fun setSubtitle(subtitle: String?) {
        subtitleView?.text = subtitle
    }

}