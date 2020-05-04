package q19.kenes_widget.model

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import java.util.*

internal data class Message(
    var type: Type = Type.OPPONENT,
    var text: String,
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
        text: String?,
        date: Long
    ) : this(type, text ?: "", fromTimestamp(date))

    constructor(
        type: Type,
        text: String?,
        date: Long,
        category: Category?
    ) : this(type, text ?: "", fromTimestamp(date), category)

    constructor(
        type: Type,
        category: Category?
    ) : this(type, "", now(), category)

    val time: String
        get() = parse(date)

    val htmlText: Spanned?
        get() {
            return if (text.isNotBlank()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(text)
                }
            } else null
        }

    enum class Type {
        SELF,
        OPPONENT,

        NOTIFICATION,

        TYPING,

        CATEGORY,
        CROSS_CHILDREN,
        RESPONSE
    }

    override fun toString(): String {
        return "Message(text=\"$text\", category=$category)"
    }

}