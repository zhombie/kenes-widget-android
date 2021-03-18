package q19.kenes_widget

import android.content.Context
import android.content.Intent
import q19.kenes_widget.api.model.Authorization
import q19.kenes_widget.api.model.DeepLink
import q19.kenes_widget.api.model.Language
import q19.kenes_widget.api.model.User
import q19.kenes_widget.data.model.toDomain
import q19.kenes_widget.data.network.socket.toDomain
import q19.kenes_widget.ui.presentation.KenesWidgetV2Activity

class KenesWidget private constructor() {

    class Builder constructor(private val context: Context) {

        private var hostname: String? = null
        private var language: Language? = null
        private var authorization: Authorization? = null
        private var user: User? = null
        private var deepLink: DeepLink? = null

        fun getContext(): Context {
            return context
        }

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

        fun getAuthorization(): Authorization? {
            return authorization
        }

        fun setAuthorization(authorization: Authorization): Builder {
            this.authorization = authorization
            return this
        }

        fun getUser(): User? {
            return user
        }

        fun setUser(user: User): Builder {
            this.user = user
            return this
        }

        fun getDeepLink(): DeepLink? {
            return deepLink
        }

        fun setDeepLink(deepLink: DeepLink): Builder {
            this.deepLink = deepLink
            return this
        }

        fun build(): Intent {
            return KenesWidgetV2Activity.newIntent(
                context,
                hostname = requireNotNull(hostname) { "Declare hostname, without it Kenes Widget won't work!" },
                language = language?.toDomain() ?: q19.kenes_widget.data.model.Language.RUSSIAN,
                authorization = authorization?.toDomain(),
                user = user?.toDomain(),
                deepLink = deepLink
            )
        }

        fun launch() {
            context.startActivity(build())
        }
    }

}