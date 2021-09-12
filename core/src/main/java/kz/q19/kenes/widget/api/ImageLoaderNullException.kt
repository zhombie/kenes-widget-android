package kz.q19.kenes.widget.api

import kz.q19.kenes.widget.core.Settings

class ImageLoaderNullException : IllegalStateException() {

    override val message: String
        get() = "${ImageLoader::class.java.simpleName} not initialized at ${Settings::class.java.simpleName}"

}