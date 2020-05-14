package q19.kenes_widget.model

import androidx.annotation.DrawableRes
import q19.kenes_widget.R

internal data class Configs(
    var opponent: Opponent = Opponent(),
    var contacts: MutableList<Contact> = mutableListOf(),
    var phones: MutableList<String> = mutableListOf(),
    var workingHours: WorkingHours = WorkingHours()
) {

    data class Opponent(
        var name: String? = null,
        var secondName: String? = null,
        var avatarUrl: String? = null
    )

    data class Contact(
        var key: String,
        var value: String
    ) {

        enum class Social(val key: String, val title: String, @DrawableRes val icon: Int) {
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
    )

}