package q19.kenes_widget.data.model

import androidx.annotation.DrawableRes
import org.json.JSONObject
import q19.kenes_widget.R
import q19.kenes_widget.util.JsonUtil.jsonObject

internal data class Configs constructor(
    val booleans: Booleans = Booleans(),
    val opponent: Opponent? = null,
    val contacts: List<Contact>? = null,
    val phones: List<String>? = null,
    val workingHours: WorkingHours? = null,
    val infoBlocks: List<InfoBlock>? = null,
    val callScopes: List<CallScope>? = null
) {

    data class Opponent constructor(
        var name: String? = null,
        var secondName: String? = null,
        var avatarUrl: String? = null,

        @DrawableRes
        val drawableRes: Int = UNDEFINED_DRAWABLE_RES
    ) {
        companion object {
            private const val UNDEFINED_DRAWABLE_RES = -1

            val DEFAULT: Opponent
                get() {
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

    data class Contact constructor(
        var key: String,
        var value: String
    ) {

        enum class Social constructor(
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

    data class WorkingHours constructor(
        val messageKk: String? = null,
        val messageRu: String? = null
    ) {

        fun getMessage(language: Language): String? {
            return when (language) {
                Language.KAZAKH -> messageKk
                Language.RUSSIAN -> messageRu
                else -> {
                    /**
                     * [messageRu] is a default value.
                     */
                    messageRu
                }
            }
        }

    }

    data class InfoBlock constructor(
        val title: I18NString,
        val description: I18NString,
        val items: List<Item>
    )

    data class I18NString constructor(
        val value: JSONObject
    ) {
        companion object {
            fun JSONObject.parse(): I18NString {
                return I18NString(this)
            }

            val NOT_FOUND: I18NString
                get() {
                    return I18NString(jsonObject {
                        put(Language.ENGLISH.key, "Nothing found :(")
                        put(Language.RUSSIAN.key, "Ничего не найдено :(")
                        put(Language.KAZAKH.key, "Ештеңе табылмады :(")
                    })
                }
        }

        fun get(language: Language): String {
            return value.optString(language.key)
        }
    }

    data class Item constructor(
        val icon: String?,
        val text: String,
        val description: I18NString,
        val action: String
    )

    data class Booleans constructor(
        val isChabotEnabled: Boolean = false,
        val isAudioCallEnabled: Boolean = false,
        val isVideoCallEnabled: Boolean = false,
        val isContactSectionsShown: Boolean = false,
        val isPhonesListShown: Boolean = false,
        val isOperatorsScoped: Boolean = false,
        val isServicesEnabled: Boolean = false
    )

    data class CallScope(
        val id: Long,
        val type: Type? = null,
        val scope: String? = null,
        val title: I18NString,
        val parentId: Long = -1L,
        val chatType: ChatType? = null,
        val action: Action? = null,
        val details: Details? = null
    ) {

        companion object {
            private const val PARENT_ID = 0L

            fun isAllParentCallScopes(callScopes: List<CallScope>?): Boolean {
                if (callScopes.isNullOrEmpty()) return false
                return callScopes.all { it.parentId == PARENT_ID }
            }

            fun getCallScopes(callScopes: List<CallScope>?, id: Long?): List<CallScope>? {
                if (callScopes.isNullOrEmpty()) return null
                return callScopes.filter { it.parentId == id }
            }

            fun getMediaCallScopes(callScopes: List<CallScope>?, id: Long = PARENT_ID): List<CallScope>? {
                return getCallScopes(
                    callScopes = callScopes?.filter {
                        it.chatType == ChatType.AUDIO || it.chatType == ChatType.VIDEO
                    },
                    id = id
                )
            }

            fun getExternalServices(callScopes: List<CallScope>?, id: Long = PARENT_ID): List<Service>? {
                return getCallScopes(
                    callScopes = callScopes?.filter {
                        it.chatType == ChatType.EXTERNAL || it.chatType == ChatType.FORM
                    },
                    id = id
                )?.map {
                    Service(
                        id = it.id,
                        type = it.type,
                        scope = it.scope,
                        title = it.title,
                        parentId = it.parentId,
                        chatType = it.chatType,
                        action = it.action,
                        details = it.details
                    )
                }
            }

            val EMPTY: CallScope
                get() {
                    return CallScope(
                        id = -1L,
                        type = null,
                        scope = null,
                        title = I18NString.NOT_FOUND,
                        parentId = -1L,
                        chatType = null,
                        action = null
                    )
                }

        }

        enum class Type constructor(val value: String) {
            FOLDER("folder"),
            LINK("link")
        }

        enum class Action constructor(val value: String) {
            AUDIO_CALL("audio_call"),
            VIDEO_CALL("video_call")
        }

        enum class ChatType constructor(val value: String) {
            AUDIO("audio"),
            VIDEO("video"),
            EXTERNAL("external"),
            FORM("form")
        }

        data class Details constructor(
            val order: Int
        )

    }

    fun clear() {
        opponent?.clear()
    }

}

internal typealias Service = Configs.CallScope