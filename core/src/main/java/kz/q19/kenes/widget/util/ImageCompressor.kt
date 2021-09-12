package kz.q19.kenes.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import kz.q19.kenes.widget.core.logging.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

internal class ImageCompressor constructor(private val context: Context) {

    companion object {
        private val TAG = ImageCompressor::class.java.simpleName
    }

    /**
     * @param imageUri the input image uri. usually "content://..."
     * @param compressFormat the output image file format
     * @param maxWidth the output image max width
     * @param maxHeight the output image max height
     * @param useMaxScale determine whether to use the bigger dimension
     * between [maxWidth] or [maxHeight]
     * @param quality the output image compress quality
     * @param minWidth the output image min width
     * @param minHeight the output image min height
     *
     * @return output image [android.net.Uri]
     */
    fun compress(
        imageUri: Uri,
        compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        maxWidth: Float = 1280F,
        maxHeight: Float = 1280F,
        useMaxScale: Boolean = true,
        quality: Int = 75,
        minWidth: Float = 150F,
        minHeight: Float = 150F
    ): Uri? {
        /**
         * Decode uri bitmap from activity result using content provider
         */
        val decoded = decodeBitmapFromUri(imageUri)

        Logger.debug(TAG, "BITMAP: ${decoded.first}")

        val options = decoded.second

        /**
         * Calculate scale factor of the bitmap relative to [maxWidth] and [maxHeight]
         */
        val scaleDownFactor: Float = calculateScaleDownFactor(
            options, useMaxScale, maxWidth, maxHeight
        )

        /**
         * Since [BitmapFactory.Options.inSampleSize] only accept value with power of 2,
         * we calculate the nearest power of 2 to the previously calculated scaleDownFactor
         * check doc [BitmapFactory.Options.inSampleSize]
         */
        setNearestInSampleSize(options, scaleDownFactor)

        /**
         * 2 things we do here with image matrix:
         * - Adjust image rotation
         * - Scale image matrix based on remaining [scaleDownFactor / [BitmapFactory.Options].inSampleSize]
         */
        val matrix: Matrix = calculateImageMatrix(imageUri, scaleDownFactor, options) ?: return null

        /**
         * Create new bitmap based on defined [BitmapFactory.Options] and calculated matrix
         */
        val newBitmap: Bitmap = generateNewBitmap(imageUri, options, matrix) ?: return null
        val newBitmapWidth = newBitmap.width
        val newBitmapHeight = newBitmap.height

        /**
         * Determine whether to scale up the image or not if the
         * image width and height is below minimum dimension
         */
        val shouldScaleUp: Boolean = shouldScaleUp(
            newBitmapWidth, newBitmapHeight, minWidth, minHeight
        )

        /**
         * Calculate the final scaleUpFactor if the image need to be scaled up.
         */
        val scaleUpFactor: Float = calculateScaleUpFactor(
            newBitmapWidth.toFloat(), newBitmapHeight.toFloat(), maxWidth, maxHeight,
            minWidth, minHeight, shouldScaleUp
        )

        /**
         * Calculate the final width and height based on final scaleUpFactor
         */
        val finalWidth: Int = finalWidth(newBitmapWidth.toFloat(), scaleUpFactor)
        val finalHeight: Int = finalHeight(newBitmapHeight.toFloat(), scaleUpFactor)

        /**
         * Generate the final bitmap, by scaling up if needed
         */
        val finalBitmap: Bitmap = scaleUpBitmapIfNeeded(
            newBitmap, finalWidth, finalHeight, scaleUpFactor, shouldScaleUp
        )

        /**
         * Compress and save image
         */
        val imageFilePath: String = compressAndSaveImage(
            finalBitmap, compressFormat, quality
        ) ?: return null

        return Uri.fromFile(File(imageFilePath))
    }

    private fun decodeBitmapFromUri(imageUri: Uri): Pair<Bitmap?, BitmapFactory.Options> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()
        return bitmap to options
    }

    private fun calculateScaleDownFactor(
        options: BitmapFactory.Options,
        useMaxScale: Boolean,
        maxWidth: Float,
        maxHeight: Float
    ): Float {
        val photoW = options.outWidth.toFloat()
        val photoH = options.outHeight.toFloat()
        val widthRatio = photoW / maxWidth
        val heightRatio = photoH / maxHeight
        var scaleFactor = if (useMaxScale) {
            max(widthRatio, heightRatio)
        } else {
            min(widthRatio, heightRatio)
        }
        if (scaleFactor < 1) {
            scaleFactor = 1f
        }
        return scaleFactor
    }

    private fun setNearestInSampleSize(
        bmOptions: BitmapFactory.Options,
        scaleFactor: Float
    ) {
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor.toInt()
        if (bmOptions.inSampleSize % 2 != 0) { // check if sample size is divisible by 2
            var sample = 1
            while (sample * 2 < bmOptions.inSampleSize) {
                sample *= 2
            }
            bmOptions.inSampleSize = sample
        }
    }

    private fun calculateImageMatrix(
        imageUri: Uri,
        scaleFactor: Float,
        options: BitmapFactory.Options
    ): Matrix? {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val exif = ExifInterface(inputStream)
        val matrix = Matrix()
        val orientation: Int = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val remainingScaleFactor = scaleFactor / options.inSampleSize.toFloat()
        if (remainingScaleFactor > 1) {
            matrix.postScale(1.0f / remainingScaleFactor, 1.0f / remainingScaleFactor)
        }
        inputStream.close()
        return matrix
    }

    private fun generateNewBitmap(
        imageUri: Uri,
        options: BitmapFactory.Options,
        matrix: Matrix
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        try {
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            if (bitmap != null) {
                val matrixScaledBitmap: Bitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (matrixScaledBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = matrixScaledBitmap
                }
            }
            inputStream?.close()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return bitmap
    }

    private fun shouldScaleUp(
        photoW: Int,
        photoH: Int,
        minWidth: Float,
        minHeight: Float
    ): Boolean {
        return (minWidth != 0F && minHeight != 0F && (photoW < minWidth || photoH < minHeight))
    }

    private fun calculateScaleUpFactor(
        photoW: Float,
        photoH: Float,
        maxWidth: Float,
        maxHeight: Float,
        minWidth: Float,
        minHeight: Float,
        shouldScaleUp: Boolean
    ): Float {
        var scaleUpFactor: Float = max(photoW / maxWidth, photoH / maxHeight)
        if (shouldScaleUp) {
            scaleUpFactor = if (photoW < minWidth && photoH > minHeight) {
                photoW / minWidth
            } else if (photoW > minWidth && photoH < minHeight) {
                photoH / minHeight
            } else {
                max(photoW / minWidth, photoH / minHeight)
            }
        }
        return scaleUpFactor
    }

    private fun finalWidth(
        photoW: Float, scaleUpFactor: Float
    ): Int {
        return (photoW / scaleUpFactor).toInt()
    }

    private fun finalHeight(
        photoH: Float, scaleUpFactor: Float
    ): Int {
        return (photoH / scaleUpFactor).toInt()
    }

    private fun scaleUpBitmapIfNeeded(
        bitmap: Bitmap,
        finalWidth: Int,
        finalHeight: Int,
        scaleUpFactor: Float,
        shouldScaleUp: Boolean
    ): Bitmap {
        val scaledBitmap: Bitmap = if (scaleUpFactor > 1 || shouldScaleUp) {
            Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        } else {
            bitmap
        }
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }

    private fun compressAndSaveImage(
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat?,
        quality: Int,
    ): String? {
        val filename = context.packageName + "_" + System.currentTimeMillis() + ".jpg"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(directory, filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(compressFormat, quality, outputStream)
        outputStream.close()
        bitmap.recycle()
        return file.absolutePath
    }

}