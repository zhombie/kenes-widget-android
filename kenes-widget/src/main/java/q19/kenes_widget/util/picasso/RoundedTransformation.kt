package q19.kenes_widget.util.picasso

import android.graphics.*
import com.squareup.picasso.Transformation

class RoundedTransformation(
    private val radius: Int
) : Transformation {

    private val key: String =
        String.format("%s_%d", RoundedTransformation::class.java.simpleName, radius)

    override fun transform(source: Bitmap?): Bitmap? {
        if (source == null || source.isRecycled) {
            return null
        }

        val output = Bitmap.createBitmap(source.width, source.height, source.config)
        val canvas = Canvas(output)
        val paint = Paint()
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true
        canvas.drawRoundRect(
            RectF(0F, 0F, output.width.toFloat(), output.height.toFloat()),
            radius.toFloat(),
            radius.toFloat(),
            paint
        )
        if (output != source) {
            source.recycle()
        }
        return output
    }

    override fun key(): String = key

}