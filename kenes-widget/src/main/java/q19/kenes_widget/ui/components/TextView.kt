package q19.kenes_widget.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.annotation.FontRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat

class TextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    fun setFont(@FontRes id: Int, style: Int = Typeface.NORMAL) {
        setTypeface(typeface, style)
        return
        ResourcesCompat.getFont(context, id, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                if (style in setOf(Typeface.NORMAL, Typeface.ITALIC, Typeface.BOLD)) {
                    setTypeface(typeface, style)
                } else {
                    setTypeface(typeface)
                }
            }

            override fun onFontRetrievalFailed(reason: Int) {}
        }, null)
    }

}