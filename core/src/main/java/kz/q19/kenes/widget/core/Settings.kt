package kz.q19.kenes.widget.core

import kz.q19.kenes.widget.api.ImageLoader
import kz.q19.kenes.widget.api.ImageLoaderNullException

internal object Settings {

    private var imageLoader: ImageLoader? = null

    fun getImageLoader(): ImageLoader =
        requireNotNull(imageLoader) { ImageLoaderNullException() }

    fun setImageLoader(imageLoader: ImageLoader) {
        this.imageLoader = imageLoader
    }

    fun clear() {
        imageLoader = null
    }

}