package q19.kenes_widget.model

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import org.json.JSONObject
import q19.kenes_widget.R
import q19.kenes_widget.util.JsonUtil.getNullableLong
import q19.kenes_widget.util.JsonUtil.optJSONArrayAsList

data class Category(
    var id: Long,
    var title: String,
    var lang: Int,
    var parentId: Long? = null,
    var photo: String? = null,
    var children: MutableList<Category> = mutableListOf(),
    var responses: MutableList<Int> = mutableListOf(),

//    Local system variables
//    var home: Boolean = false,
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
            resources.getDimension(R.dimen.kenes_child_border_radius),
            Background.Stroke(
                resources.getDimensionPixelOffset(R.dimen.kenes_child_stroke_width),
                getColorWithAlpha(0.3F)
            ),
            getColorWithAlpha(0.06F)
        ))
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Category) return false
        if (id == other.id && parentId == other.parentId) return true
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "Category(id=$id, title=\"$title\", parentId=$parentId, children=$children, responses=$responses)"
    }

}


internal fun parse(jsonObject: JSONObject): Category {
    return Category(
        id = jsonObject.optLong("id"),
        title = jsonObject.optString("title").trim(),
        lang = jsonObject.optInt("lang"),
        parentId = jsonObject.getNullableLong("parent_id"),
        photo = jsonObject.optString("photo"),
        responses = jsonObject.optJSONArrayAsList("responses")
    )
}