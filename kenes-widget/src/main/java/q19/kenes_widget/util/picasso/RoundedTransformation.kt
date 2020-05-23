package q19.kenes_widget.util.picasso

import android.graphics.*
import com.squareup.picasso.Transformation

/**
 * Rounded corner transformation for [com.squareup.picasso].
 *
 *
 *
 * Modified code taken from https://gist.github.com/aprock/6213395
 *
 *
 * enables hardware accelerated rounded corners <br></br>
 * original idea here :
 * http://www.curious-creature.org/2012/12/11/android-recipe-1-image-with-rounded-corners/
 */
internal class RoundedTransformation(
    private val radius: Int,
    private val margin: Int
) : Transformation {

    companion object {
        /**
         * Prepares a path for rounded corner selectively.
         *
         *
         * Source taken from http://stackoverflow.com/a/35668889/6635889 <br></br>
         * Usage:
         * <pre>
         * Path path = RoundedRect(0, 0, fwidth , fheight , 5,5, false, true, true, false);
         * canvas.drawPath(path, myPaint);
        </pre> *
         *
         * @param leftX The X coordinate of the left side of the rectangle
         * @param topY The Y coordinate of the top of the rectangle
         * @param rightX The X coordinate of the right side of the rectangle
         * @param bottomY The Y coordinate of the bottom of the rectangle
         * @param rx The x-radius of the oval used to round the corners
         * @param ry The y-radius of the oval used to round the corners
         * @param topLeft
         * @param topRight
         * @param bottomRight
         * @param bottomLeft
         * @return
         */
        fun RoundedRect(
            leftX: Float,
            topY: Float,
            rightX: Float,
            bottomY: Float,
            rx: Float,
            ry: Float,
            topLeft: Boolean,
            topRight: Boolean,
            bottomRight: Boolean,
            bottomLeft: Boolean
        ): Path {
            var mRx = rx
            var mRy = ry
            val path = Path()
            if (mRx < 0) mRx = 0f
            if (mRy < 0) mRy = 0f
            val width = rightX - leftX
            val height = bottomY - topY
            if (mRx > width / 2) mRx = width / 2
            if (mRy > height / 2) mRy = height / 2
            val widthMinusCorners = width - 2 * mRx
            val heightMinusCorners = height - 2 * mRy
            path.moveTo(rightX, topY + mRy)
            if (topRight) path.rQuadTo(0f, -mRy, -mRx, -mRy) //top-right corner
            else {
                path.rLineTo(0f, -mRy)
                path.rLineTo(-mRx, 0f)
            }
            path.rLineTo(-widthMinusCorners, 0f)
            if (topLeft) path.rQuadTo(-mRx, 0f, -mRx, mRy) //top-left corner
            else {
                path.rLineTo(-mRx, 0f)
                path.rLineTo(0f, mRy)
            }
            path.rLineTo(0f, heightMinusCorners)
            if (bottomLeft) path.rQuadTo(0f, mRy, mRx, mRy) //bottom-left corner
            else {
                path.rLineTo(0f, mRy)
                path.rLineTo(mRx, 0f)
            }
            path.rLineTo(widthMinusCorners, 0f)
            if (bottomRight) path.rQuadTo(mRx, 0f, mRx, -mRy) //bottom-right corner
            else {
                path.rLineTo(mRx, 0f)
                path.rLineTo(0f, -mRy)
            }
            path.rLineTo(0f, -heightMinusCorners)
            path.close() //Given close, last lineto can be removed.
            return path
        }
    }

    /**
     * Creates rounded transformation for top or bottom corners.
     *
     * @param radius radius is corner radii in dp
     * @param margin margin is the board in dp
     * @param topCornersOnly Rounded corner for top corners only.
     * @param bottomCornersOnly Rounded corner for bottom corners only.
     */
    constructor(
        radius: Int,
        margin: Int,
        topCornersOnly: Boolean,
        bottomCornersOnly: Boolean
    ) : this(radius, margin) {
        topCorners = topCornersOnly
        bottomCorners = bottomCornersOnly
    }

    private val KEY: String = "rounded_$radius$margin"
    private var topCorners = true
    private var bottomCorners = true

    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val margin = margin.toFloat()
        val radius = radius.toFloat()
        if (topCorners && bottomCorners) {
            // Uses native method to draw symmetric rounded corners
            canvas.drawRoundRect(
                RectF(margin, margin, source.width - margin, (source.height - margin)),
                radius,
                radius,
                paint
            )
        } else {
            // Uses custom path to generate rounded corner individually
            canvas.drawPath(
                RoundedRect(
                    margin,
                    margin,
                    source.width - margin,
                    source.height - margin,
                    radius,
                    radius,
                    topCorners,
                    topCorners,
                    bottomCorners,
                    bottomCorners
                ), paint
            )
        }
        if (source != output) {
            source.recycle()
        }
        return output
    }

    override fun key(): String {
        return KEY
    }

}