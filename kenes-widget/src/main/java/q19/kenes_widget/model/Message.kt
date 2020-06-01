package q19.kenes_widget.model

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import java.util.*

internal data class Message(
    var id: String? = null,
    var type: Type = Type.OPPONENT,
    var text: String,
    var media: Media? = null,
    var attachments: List<Attachment>? = null,
    var date: Calendar = now(),
    var category: Category? = null
) {

    companion object {
        fun fromTimestamp(timestamp: Long): Calendar {
            val calendar = Calendar.getInstance()
            if (timestamp == 0L) {
                return calendar
            }
            calendar.timeInMillis = timestamp * 1000L
            return calendar
        }

        fun now(): Calendar {
            return Calendar.getInstance()
        }

        fun parse(calendar: Calendar): String {
            return DateFormat.format("HH:MM", calendar).toString()
        }
    }

    constructor(
        type: Type,
        text: String? = null,
        media: Media? = null,
        attachments: List<Attachment>? = null,
        timestamp: Long? = null,
        category: Category? = null
    ) : this(
        id = null,
        type = type,
        text = text?.trim() ?: "",
        media = media,
        attachments = attachments,
        date = timestamp?.let { fromTimestamp(it) } ?: now(),
        category = category
    )

    val time: String
        get() = parse(date)

    val htmlText: Spanned?
        get() {
            return if (text.isNotBlank()) {
                val text = text.replace("(<br>|<br/>)*$".toRegex(), "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(text)
                }
            } else null
        }

    enum class Type {
        USER,
        OPPONENT,

        NOTIFICATION,

        TYPING,

        CATEGORY,
        CROSS_CHILDREN,
        RESPONSE
    }

    override fun toString(): String {
        return "Message(type=$type, text=\"$text\", category=$category)"
    }

}