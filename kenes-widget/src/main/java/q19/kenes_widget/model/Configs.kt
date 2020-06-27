package q19.kenes_widget.model

import androidx.annotation.DrawableRes
import org.json.JSONObject
import q19.kenes_widget.R

internal data class Configs(
    var isAudioCallEnabled: Boolean = false,
    var isVideoCallEnabled: Boolean = false,
    var isContactSectionsShown: Boolean = false,
    var opponent: Opponent = Opponent(),
    var contacts: List<Contact> = listOf(),
    var phones: List<String> = listOf(),
    var workingHours: WorkingHours = WorkingHours(),
    var infoBlocks: List<InfoBlock>? = null
) {

    data class Opponent(
        var name: String? = null,
        var secondName: String? = null,
        var avatarUrl: String? = null,

        @DrawableRes
        var drawableRes: Int = UNDEFINED_DRAWABLE_RES
    ) {
        companion object {
            private const val UNDEFINED_DRAWABLE_RES = -1
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
            VK("vk", "ВКонтакте", R.drawable.kenes_ic_vk)
        }

        val social: Social?
            get() {
                return when(key) {
                    Social.FACEBOOK.key -> Social.FACEBOOK
                    Social.TELEGRAM.key -> Social.TELEGRAM
                    Social.VK.key -> Social.VK
                    else -> null
                }
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
        var messageKk: String? = null,
        var messageRu: String? = null
    ) {
        fun clear() {
            messageKk = null
            messageRu = null
        }
    }

    data class InfoBlock(
        val title: I18NString,
        val description: I18NString,
        val items: List<Item>
    )

    class I18NString(
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

    fun clear() {
        opponent.clear()
        workingHours.clear()
    }

}