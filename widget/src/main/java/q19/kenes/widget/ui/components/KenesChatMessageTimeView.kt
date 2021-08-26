package q19.kenes.widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.textview.MaterialTextView
import kz.q19.utils.android.dp2Px
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class KenesChatMessageTimeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr, defStyleRes) {

    init {
        includeFontPadding = false

        setLineSpacing(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                0.0F,
                resources.displayMetrics
            ),
            1.0F
        )

        TextViewCompat.setTextAppearance(this, R.style.Kenes_Widget_TextAppearance_Message_Time)

        setPadding(
            7.5F.dp2Px().roundToInt(),
            2.5F.dp2Px().roundToInt(),
            7.5F.dp2Px().roundToInt(),
            2.5F.dp2Px().roundToInt()
        )
    }

    fun setDefaultStyle() {
        background = null

        setTextColor(ContextCompat.getColor(context, R.color.kenes_gray))
    }

    fun setInvertedStyle() {
        background = AppCompatResources.getDrawable(context, R.drawable.bg_rounded_alpha_black)

        setTextColor(ContextCompat.getColor(context, R.color.kenes_white))
    }

}