package kz.q19.kenes.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kz.q19.kenes.widget.api.ImageLoader
import kz.q19.kenes.widget.imageloader.R

class ConcatCoilImageLoader constructor(
    context: Context,
    isLoggingEnabled: Boolean = false
) : CoilImageLoader(context, isLoggingEnabled), ImageLoader {

    override fun loadStandardImage(context: Context, imageView: ImageView, uri: Uri) {
        ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(true)
            .data(uri)
            .error(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(512, 512)
            .target(imageView)
            .build()
            .enqueue()
    }

    override fun cleanup() {
        clearCache()
    }

}