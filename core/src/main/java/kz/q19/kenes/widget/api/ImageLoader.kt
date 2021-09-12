package kz.q19.kenes.widget.api

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import kz.zhombie.museum.PaintingLoader

interface ImageLoader : PaintingLoader {
    fun loadStandardImage(context: Context, imageView: ImageView, uri: Uri)

    fun cleanup()
}