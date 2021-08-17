package q19.kenes.widget.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textview.MaterialTextView
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.core.logging.Logger
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

    companion object {
        private val TAG = Toolbar::class.java.simpleName
    }

    private var leftButton: MaterialButton? = null
    private var imageView: ShapeableImageView? = null
    private var titleView: MaterialTextView? = null
    private var subtitleView: MaterialTextView? = null
    private var rightButton: MaterialButton? = null

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
            setMargins(5F.dp2Px().roundToInt(), 0, 0, 0)
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
        subtitleView?.layoutParams =
            MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
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

    fun setLeftButtonEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            if (getChildAt(childCount) is MaterialButton || rightButton != null) {
                Logger.error(TAG, "Right button is already enabled, choose only one of them")
                return
            }

            if (leftButton == null) {
                leftButton = MaterialButton(context, null, R.style.Widget_MaterialComponents_Button_Icon)
                leftButton?.id = View.generateViewId()
                leftButton?.layoutParams = LayoutParams(40F.dp2Px().roundToInt(), 40F.dp2Px().roundToInt()).apply {
                    setMargins(0, 0, 0, 0)
                }
                leftButton?.elevation = 0F
                leftButton?.insetBottom = 0
                leftButton?.insetTop = 0
                leftButton?.minWidth = 0
                leftButton?.minHeight = 0
                leftButton?.maxHeight = 0
                leftButton?.maxWidth = 0
                leftButton?.setBackgroundColor(ContextCompat.getColor(context, R.color.kenes_transparent))
                leftButton?.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                leftButton?.iconPadding = 0
                leftButton?.iconSize = 18F.dp2Px().roundToInt()
                leftButton?.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.kenes_black))
                leftButton?.isAllCaps = false
                leftButton?.setPadding(0, 0, 0, 0)
                leftButton?.text = null
                leftButton?.shapeAppearanceModel = ShapeAppearanceModel
                    .builder(context, R.style.Kenes_Widget_ShapeAppearance_Circle, 0)
                    .build()
            }

            if (getChildAt(0) !is MaterialButton) {
                addView(leftButton, 0)
            }
        } else {
            if (getChildAt(0) is MaterialButton) {
                if (leftButton != null) {
                    removeView(leftButton)
                }
            }
        }
    }

    fun setRightButtonEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            if (getChildAt(0) is MaterialButton || leftButton != null) {
                Logger.error(TAG, "Left button is already enabled, choose only one of them")
                return
            }

            if (rightButton == null) {
                rightButton = MaterialButton(context, null, R.style.Widget_MaterialComponents_Button_Icon)
                rightButton?.id = View.generateViewId()
                rightButton?.layoutParams = LayoutParams(40F.dp2Px().roundToInt(), 40F.dp2Px().roundToInt()).apply {
                    setMargins(0, 0, 0, 0)
                }
                rightButton?.elevation = 0F
                rightButton?.insetBottom = 0
                rightButton?.insetTop = 0
                rightButton?.minWidth = 0
                rightButton?.minHeight = 0
                rightButton?.maxHeight = 0
                rightButton?.maxWidth = 0
                rightButton?.setBackgroundColor(ContextCompat.getColor(context, R.color.kenes_transparent))
                rightButton?.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                rightButton?.iconPadding = 0
                rightButton?.iconSize = 18F.dp2Px().roundToInt()
                rightButton?.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.kenes_black))
                rightButton?.isAllCaps = false
                rightButton?.setPadding(0, 0, 0, 0)
                rightButton?.text = null
                rightButton?.shapeAppearanceModel = ShapeAppearanceModel
                    .builder(context, R.style.Kenes_Widget_ShapeAppearance_Circle, 0)
                    .build()
            }

            if (getChildAt(childCount) !is MaterialButton) {
                addView(rightButton, childCount)
            }
        } else {
            if (getChildAt(childCount) is MaterialButton) {
                if (rightButton != null) {
                    removeView(rightButton)
                }
            }
        }
    }

    fun setLeftButtonBackgroundTint(tintList: ColorStateList?) {
        leftButton?.backgroundTintList = tintList
    }

    fun setLeftButtonBackgroundTint(@ColorRes backgroundTintResourceId: Int) {
        leftButton?.backgroundTintList = AppCompatResources.getColorStateList(context, backgroundTintResourceId)
    }

    fun setLeftButtonIcon(drawable: Drawable?) {
        leftButton?.icon = drawable
    }

    fun setLeftButtonIcon(@DrawableRes iconResourceId: Int) {
        leftButton?.setIconResource(iconResourceId)
    }

    fun setLeftButtonIconTint(iconTint: ColorStateList?) {
        leftButton?.iconTint = iconTint
    }

    fun setLeftButtonIconTint(@ColorRes iconTintResourceId: Int) {
        leftButton?.setIconTintResource(iconTintResourceId)
    }

    fun setRightButtonBackgroundTint(tintList: ColorStateList?) {
        rightButton?.backgroundTintList = tintList
    }

    fun setRightButtonBackgroundTint(@ColorRes backgroundTintResourceId: Int) {
        rightButton?.backgroundTintList = AppCompatResources.getColorStateList(context, backgroundTintResourceId)
    }

    fun setRightButtonIcon(drawable: Drawable?) {
        rightButton?.icon = drawable
    }

    fun setRightButtonIcon(@DrawableRes iconResourceId: Int) {
        rightButton?.setIconResource(iconResourceId)
    }

    fun setRightButtonIconTint(iconTint: ColorStateList?) {
        rightButton?.iconTint = iconTint
    }

    fun setRightButtonIconTint(@ColorRes iconTintResourceId: Int) {
        rightButton?.setIconTintResource(iconTintResourceId)
    }

    fun setLeftButtonOnClickListener(listener: OnClickListener?) {
        leftButton?.setOnClickListener(listener)
    }

    fun setRightButtonOnClickListener(listener: OnClickListener?) {
        rightButton?.setOnClickListener(listener)
    }

}