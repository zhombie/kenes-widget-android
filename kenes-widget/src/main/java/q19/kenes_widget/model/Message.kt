package q19.kenes_widget.model

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
        text: String,
        date: Long
    ) : this(type, text, fromTimestamp(date))

    constructor(
        type: Type,
        category: Category?
    ) : this(type, "", now(), category)

    constructor(
        type: Type
    ) : this(type, "", now())

    val time: String
        get() = parse(date)

    enum class Type {
        SELF,
        OPPONENT,

        NOTIFICATION,

        TYPING,

        CATEGORY,
        CROSS_CHILDREN
    }

    override fun toString(): String {
        return "Message(category=$category)"
    }

}