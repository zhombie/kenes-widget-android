package q19.kenes_widget.util

import android.graphics.Bitmap
import android.widget.ImageView
import q19.imageviewer.ImageViewer

internal fun ImageView.showFullscreenImage(bitmap: Bitmap) {
    showFullScreenImage(bitmap) { imageView, innerBitmap ->
        imageView.setImageBitmap(innerBitmap)
    }
}

internal fun ImageView.showFullscreenImage(imageUrl: String) {
    showFullScreenImage(imageUrl) { imageView, url ->
        imageView.loadImage(url)
    }
}

private fun <T> ImageView.showFullScreenImage(
    anything: T,
    callback: (imageView: ImageView, anything: T) -> Unit
) {
    ImageViewer.Builder(context, listOf(anything)) { imageView, any ->
        callback(imageView, any)
    }
        .allowZooming(true)
        .allowSwipeToDismiss(true)
        .withHiddenStatusBar(true)
        .show()
}