package q19.kenes.widget

import android.content.Context
import android.content.Intent
import q19.kenes.widget.api.ImageLoader
import q19.kenes.widget.api.ImageLoaderNullException
import q19.kenes.widget.api.Language
import q19.kenes.widget.core.Settings
import q19.kenes.widget.ui.presentation.KenesWidgetActivity
import java.io.Serializable

class KenesWidget private constructor() {

    companion object {
        val SUPPORTED_LOCALES = listOf(
            kz.q19.domain.model.language.Language.RUSSIAN.locale,
            kz.q19.domain.model.language.Language.KAZAKH.locale
        )
    }

    class Builder constructor(private val context: Context) {

        data class User constructor(
            val firstName: String? = null,
            val lastName: String? = null,
            val phoneNumber: String? = null
        ) : Serializable

        private var hostname: String? = null
        private var language: Language? = null
        private var imageLoader: ImageLoader? = null
        private var user: User? = null

        fun getHostname(): String? {
            return hostname
        }

        fun setHostname(hostname: String): Builder {
            this.hostname = hostname
            return this
        }

        fun getLanguage(): Language? {
            return language
        }

        fun setLanguage(language: Language): Builder {
            this.language = language
            return this
        }

        fun setImageLoader(imageLoader: ImageLoader): Builder {
            this.imageLoader = imageLoader
            return this
        }

        fun getUser(): User? {
            return user
        }

        fun setUser(user: User): Builder {
            this.user = user
            return this
        }

        fun build(): Intent {
            Settings.clear()
            Settings.setImageLoader(imageLoader ?: throw ImageLoaderNullException())

            return KenesWidgetActivity.newIntent(
                context = context,
                hostname = requireNotNull(hostname) { "Declare hostname, without it widget won't work!" },
                languageKey = Language.map(language).key,
                user = user
            )
        }

        fun launch(): Intent {
            val intent = build()
            context.startActivity(intent)
            return intent
        }
    }

}