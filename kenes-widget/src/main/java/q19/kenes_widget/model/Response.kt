package q19.kenes_widget.model

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import q19.kenes_widget.R

internal open class Response(
    open var id: Long,
    open var title: String,
    open var lang: Int,
    open var parentId: Long? = null,
    open var photo: String? = null,
    open var responses: MutableList<Response> = mutableListOf(),

    @ColorInt var color: Int = 0
) {

    class Background(
        var cornerRadius: Float,
        var stroke: Stroke,
        @ColorInt var color: Int
    ) {

        class Stroke(
            var width: Int,
            @ColorInt var color: Int
        )

    }

    private fun getColorWithAlpha(percentage: Float): Int {
        return try {
            ColorUtils.setAlphaComponent(color, (percentage * 255).toInt())
        } catch (e: Exception) {
            e.printStackTrace()
            color
        }
    }

    private fun buildBackground(background: Background): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.cornerRadius = background.cornerRadius
        drawable.setStroke(background.stroke.width, background.stroke.color)
        drawable.setColor(background.color)
        return drawable
    }

    fun getDefaultBackground(resources: Resources): GradientDrawable {
        return buildBackground(Background(
            resources.getDimension(R.dimen.kenes_section_border_radius),
            Background.Stroke(
                resources.getDimensionPixelOffset(R.dimen.kenes_section_stroke_width),
                getColorWithAlpha(0.3F)
            ),
            getColorWithAlpha(0.06F)
        ))
    }

    override fun toString(): String {
        return "Response(id=$id, title=\"$title\", parentId=$parentId, sections=$responses])"
    }

}