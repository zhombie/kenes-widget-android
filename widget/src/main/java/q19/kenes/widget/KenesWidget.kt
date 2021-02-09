package q19.kenes.widget

import android.content.Context
import android.content.Intent
import kz.q19.common.locale.core.LanguageSetting
import q19.kenes.widget.ui.presentation.KenesWidgetActivity
import java.io.Serializable

class KenesWidget private constructor() {

    class Builder {

        enum class Language {
            KAZAKH,
            RUSSIAN
        }

        data class User constructor(
            val firstName: String? = null,
            val lastName: String? = null,
            val phoneNumber: String? = null
        ) : Serializable

        private var hostname: String? = null
        private var language: Language? = null
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

        fun getUser(): User? {
            return user
        }

        fun setUser(user: User): Builder {
            this.user = user
            return this
        }

        fun buildIntent(context: Context): Intent {
            val language = when (language) {
                Language.KAZAKH -> kz.q19.domain.model.language.Language.KAZAKH
                Language.RUSSIAN -> kz.q19.domain.model.language.Language.RUSSIAN
                else -> null
            }
            if (language?.locale != null) {
                LanguageSetting.setLanguage(context, language.locale)
            }
            return KenesWidgetActivity.newIntent(
                context,
                hostname = requireNotNull(hostname) { "Declare hostname, without it widget won't work!" },
                language = language,
                user = user
            )
        }
    }

}