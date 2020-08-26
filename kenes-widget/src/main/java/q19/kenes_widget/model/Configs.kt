package q19.kenes_widget.model

import androidx.annotation.DrawableRes
import org.json.JSONObject
import q19.kenes_widget.R

data class Configs(
    val booleans: Booleans = Booleans(),
    val opponent: Opponent? = null,
    val contacts: List<Contact>? = null,
    val phones: List<String>? = null,
    val workingHours: WorkingHours? = null,
    val infoBlocks: List<InfoBlock>? = null,
    val callScopes: List<CallScope>? = null
) {

    data class Opponent(
        var name: String? = null,
        var secondName: String? = null,
        var avatarUrl: String? = null,

        @DrawableRes
        val drawableRes: Int = UNDEFINED_DRAWABLE_RES
    ) {
        companion object {
            private const val UNDEFINED_DRAWABLE_RES = -1

            fun getDefault(): Opponent {
                return Opponent(
                    secondName = "Smart Bot",
                    drawableRes = R.drawable.kenes_ic_robot
                )
            }
        }

        val isDrawableResAvailable: Boolean
            get() = drawableRes != UNDEFINED_DRAWABLE_RES

        fun clear() {
            name = null
            secondName = null
            avatarUrl = null
        }
    }

    data class Contact(
        var key: String,
        var value: String
    ) {

        enum class Social(
            val key: String,
            val title: String,
            @DrawableRes val icon: Int
        ) {
            FACEBOOK("fb", "Facebook", R.drawable.kenes_ic_messenger),
            TELEGRAM("tg", "Telegram", R.drawable.kenes_ic_telegram),
            TWITTER("tw", "Twitter", R.drawable.kenes_ic_twitter),
            VK("vk", "ВКонтакте", R.drawable.kenes_ic_vk)
        }

        val social: Social?
            get() = when(key) {
                Social.FACEBOOK.key -> Social.FACEBOOK
                Social.TELEGRAM.key -> Social.TELEGRAM
                Social.TWITTER.key -> Social.TWITTER
                Social.VK.key -> Social.VK
                else -> null
            }

        val url: String
            get() {
                var url = value
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://$url"
                }
                return url
            }

    }

    data class WorkingHours(
        val messageKk: String? = null,
        val messageRu: String? = null
    ) {

        fun getMessage(language: Language): String? {
            return when (language) {
                Language.Kazakh -> messageKk
                Language.Russian -> messageRu
                else -> {
                    /**
                     * [messageRu] is a default value.
                     */
                    messageRu
                }
            }
        }

    }

    data class InfoBlock(
        val title: I18NString,
        val description: I18NString,
        val items: List<Item>
    )

    data class I18NString(
        val value: JSONObject
    ) {
        companion object {
            fun JSONObject.parse(): I18NString {
                return I18NString(this)
            }
        }

        fun get(language: Language): String {
            return value.optString(language.key)
        }
    }

    data class Item(
        val icon: String?,
        val text: String,
        val description: I18NString,
        val action: String
    )

    data class Booleans(
        val isChabotEnabled: Boolean = false,
        val isAudioCallEnabled: Boolean = false,
        val isVideoCallEnabled: Boolean = false,
        val isContactSectionsShown: Boolean = false,
        val isPhonesListShown: Boolean = false,
        val isOperatorsScoped: Boolean = false
    )

    data class CallScope(
        val id: Long,
        val type: String,
        val scope: String? = null,
        val title: I18NString,
        val parentId: Long,
        val chatType: String,
        val action: String? = null
    ) {

        companion object {
            private const val PARENT_ID = 0L

            fun getParentCallScopes(callScopes: List<CallScope>?): List<CallScope>? {
                if (callScopes.isNullOrEmpty()) return null
                return callScopes
                    .filter { it.chatType == "audio" || it.chatType == "video" }
                    .filter { it.parentId == PARENT_ID }
            }

            fun isAllParentCallScopes(callScopes: List<CallScope>?): Boolean {
                if (callScopes.isNullOrEmpty()) return false
                return callScopes.all { it.parentId == PARENT_ID }
            }
        }

    }

    fun clear() {
        opponent?.clear()
    }

}