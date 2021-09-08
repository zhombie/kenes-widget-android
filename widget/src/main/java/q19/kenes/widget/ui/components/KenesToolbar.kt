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
import android.widget.Space
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textview.MaterialTextView
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.util.loadImage
import q19.kenes.widget.util.picasso.CircleTransformation
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class KenesToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = KenesToolbar::class.java.simpleName

        private val MAX_ELEVATION = 2F.dp2Px()
    }

    private var leftButton: MaterialButton? = null
    private var imageView: ShapeableImageView? = null
    private var titleView: MaterialTextView? = null
    private var subtitleView: MaterialTextView? = null
    private var rightButton: MaterialButton? = null

    init {
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        setPadding(16F.dp2Px().roundToInt(), 0, 16F.dp2Px().roundToInt(), 0)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.KenesToolbar)

        val hasUserInfo = try {
            typedArray.getBoolean(R.styleable.KenesToolbar_kenesHasContent, false)
        } catch (e: Exception) {
            false
        }

        if (hasUserInfo) {
            val titleTextColor = try {
                typedArray.getColor(
                    R.styleable.KenesToolbar_kenesTitleTextColor,
                    ContextCompat.getColor(context, R.color.kenes_dark_charcoal)
                )
            } catch (e: Exception) {
                ContextCompat.getColor(context, R.color.kenes_dark_charcoal)
            }

            val subtitleTextColor = try {
                typedArray.getColor(
                    R.styleable.KenesToolbar_kenesSubtitleTextColor,
                    ContextCompat.getColor(context, R.color.kenes_dark_gray)
                )
            } catch (e: Exception) {
                ContextCompat.getColor(context, R.color.kenes_dark_gray)
            }

            imageView = buildImageView()
            addView(imageView)

            val linearLayout = buildLinearLayout(titleTextColor, subtitleTextColor)
            addView(linearLayout)
        } else {
            Space(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1F)
                addView(this)
            }
        }

        typedArray.recycle()
    }

    override fun setElevation(elevation: Float) {
//        Logger.debug(TAG, "setElevation() -> $elevation")

        if (elevation != this@KenesToolbar.elevation) {
            var finalElevation = elevation
            if (finalElevation < 0) {
                finalElevation = 0F
            }
            if (finalElevation > MAX_ELEVATION) {
                finalElevation = MAX_ELEVATION
            }
            super.setElevation(finalElevation)
        }
    }

    private fun buildImageView(): ShapeableImageView {
        return ShapeableImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(45F.dp2Px().roundToInt(), 45F.dp2Px().roundToInt())
            setContentPadding(
                5F.dp2Px().roundToInt(),
                5F.dp2Px().roundToInt(),
                5F.dp2Px().roundToInt(),
                5F.dp2Px().roundToInt()
            )
            shapeAppearanceModel = ShapeAppearanceModel
                .builder(context, R.style.Kenes_Widget_ShapeAppearance_Circle, 0)
                .build()
            strokeColor = ColorStateList.valueOf(Color.parseColor("#1ABDBDBD"))
            strokeWidth = 0.5F.dp2Px()
        }
    }

    private fun buildLinearLayout(
        @ColorInt titleTextColor: Int,
        @ColorInt subtitleTextColor: Int
    ): LinearLayout {
        val linearLayout = LinearLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1F).apply {
                setMargins(5F.dp2Px().roundToInt(), 0, 0, 0)
            }
            orientation = VERTICAL
        }

        titleView = MaterialTextView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            includeFontPadding = false
            letterSpacing = 0F
            maxLines = 1
            isSingleLine = true
            isAllCaps = false
            setTextColor(titleTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)

            linearLayout.addView(this)
        }

        subtitleView = MaterialTextView(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 1F.dp2Px().roundToInt(), 0, 0)
            }
            setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            includeFontPadding = false
            letterSpacing = 0F
            maxLines = 1
            isSingleLine = true
            isAllCaps = false
            setTextColor(subtitleTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10F)

            linearLayout.addView(this)
        }

        return linearLayout
    }

    fun isLeftButtonEnabled(): Boolean {
        return leftButton != null && indexOfChild(leftButton) > -1
    }

    fun isRightButtonEnabled(): Boolean {
        return rightButton != null && indexOfChild(rightButton) > -1
    }

    fun showImage(imageUrl: String?) {
        imageView?.loadImage(imageUrl, transformation = CircleTransformation())
    }

    fun showImage(@DrawableRes resId: Int) {
        imageView?.setImageResource(resId)
    }

    fun showImage(drawable: Drawable?) {
        imageView?.setImageDrawable(drawable)
    }

    fun setTitle(title: String?) {
        titleView?.text = title
    }

    fun setSubtitle(subtitle: String?) {
        subtitleView?.text = subtitle
    }

    fun setImageContentPadding(@Dimension padding: Int) {
        imageView?.setContentPadding(padding, padding, padding, padding)
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        titleView?.setTextColor(color)
    }

    fun setSubtitleTextColor(@ColorInt color: Int) {
        subtitleView?.setTextColor(color)
    }

    fun setLeftButtonEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            removeView(leftButton)
            leftButton = null

            if (leftButton == null) {
                leftButton = KenesIconButton(context).apply {
                    id = View.generateViewId()
                    layoutParams = LayoutParams(40F.dp2Px().roundToInt(), 40F.dp2Px().roundToInt()).apply {
                        setMargins(0, 0, 0, 0)
                    }
                    setPadding(10F.dp2Px().roundToInt())
                    leftButton?.iconSize = 22F.dp2Px().roundToInt()
                    leftButton?.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.kenes_black))
                }
            }

            addView(leftButton, 0)
        } else {
            removeView(leftButton)
            leftButton = null
        }
    }

    fun setRightButtonEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            removeView(rightButton)
            rightButton = null

            if (rightButton == null) {
                rightButton = KenesIconButton(context).apply {
                    id = View.generateViewId()
                    layoutParams = LayoutParams(40F.dp2Px().roundToInt(), 40F.dp2Px().roundToInt()).apply {
                        setMargins(0, 0, 0, 0)
                    }
                    setPadding(10F.dp2Px().roundToInt())
                    iconSize = 22F.dp2Px().roundToInt()
                    iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.kenes_black))
                }
            }

            addView(rightButton, childCount)
        } else {
            removeView(rightButton)
            rightButton = null
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

    fun reveal() {
        alpha = 0F
        animate()
            .alpha(1F)
            .setDuration(300L)
            .start()
    }

    fun setLeftButtonOnClickListener(listener: OnClickListener?) {
        leftButton?.setOnClickListener(listener)
    }

    fun setRightButtonOnClickListener(listener: OnClickListener?) {
        rightButton?.setOnClickListener(listener)
    }

}