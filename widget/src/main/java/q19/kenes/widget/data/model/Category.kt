package q19.kenes.widget.data.model

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.core.graphics.ColorUtils
import q19.kenes_widget.R

@Keep
internal data class Category constructor(
    val id: Long,
    val title: String,
    val lang: Int,
    val parentId: Long? = null,
    val photo: String? = null,
    val children: MutableList<Category> = mutableListOf(),
    val responses: MutableList<Int> = mutableListOf(),
    val config: Config? = null,

//    Local system variables
//    var home: Boolean = false,
    @ColorInt var color: Int = 0
) {

    companion object {
        val EMPTY: Category
            get() {
                return Category(
                    id = -1,
                    title = Configs.I18NString.NOT_FOUND.get(Language.DEFAULT),
                    lang = Language.DEFAULT.identificator,
                    parentId = null,
                    photo = null,
                    children = mutableListOf(),
                    responses = mutableListOf(),
                    config = null
                )
            }
    }

    @Keep
    data class Background constructor(
        val cornerRadius: Float,
        val stroke: Stroke,
        @ColorInt val color: Int
    ) {

        @Keep
        data class Stroke constructor(
            val width: Int,
            @ColorInt var color: Int
        )

    }

    @Keep
    data class Config constructor(
        val order: Int
    )

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
        return buildBackground(
            Background(
            resources.getDimension(R.dimen.kenes_child_border_radius),
            Background.Stroke(
                resources.getDimensionPixelOffset(R.dimen.kenes_child_stroke_width),
                getColorWithAlpha(0.3F)
            ),
            getColorWithAlpha(0.06F)
        )
        )
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

}