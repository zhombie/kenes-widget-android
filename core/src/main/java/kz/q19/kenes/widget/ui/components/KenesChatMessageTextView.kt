package kz.q19.kenes.widget.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import kz.q19.utils.android.dp2Px
import kz.q19.kenes.widget.R
import kotlin.math.roundToInt

internal class KenesChatMessageTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
    defStyleRes: Int = 0
) : KenesTextView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = KenesTextView::class.java.simpleName
    }

    init {
        isAllCaps = false

        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            context.resources.getDimension(R.dimen.kenes_chat_message_text_size)
        )

        minWidth = 60F.dp2Px().roundToInt()

        setPadding(
            12F.dp2Px().roundToInt(),
            7F.dp2Px().roundToInt(),
            12F.dp2Px().roundToInt(),
            8F.dp2Px().roundToInt(),
        )

        enableAutoLinkMask()
        enableLinkMovementMethod()
    }

}