package q19.kenes_widget.util

import android.graphics.Bitmap
import android.widget.ImageView
import q19.imageviewer.ImageViewer

internal fun ImageView.showFullscreenImage(bitmap: Bitmap) {
    ImageViewer.Builder(context, listOf(bitmap)) { imageView, bm ->
        imageView.setImageBitmap(bm)
    }
        .allowZooming(true)
        .allowSwipeToDismiss(true)
        .withHiddenStatusBar(true)
        .show()
}

internal fun ImageView.showFullscreenImage(imageUrl: String) {
    ImageViewer.Builder(context, listOf(imageUrl)) { imageView, url ->
        imageView.loadImage(url)
    }
        .allowZooming(true)
        .allowSwipeToDismiss(true)
        .withHiddenStatusBar(true)
        .show()
}