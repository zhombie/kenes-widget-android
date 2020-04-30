package q19.kenes_widget.models

import android.text.format.DateFormat
import java.util.*

internal data class Message(
    var type: Type = Type.OPPONENT,
    var text: String,
    var date: Calendar = now()
) {

    internal companion object {
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

    internal constructor(
        type: Type,
        text: String,
        date: Long
    ) : this(type, text, fromTimestamp(date))

    internal constructor(
        type: Type
    ) : this(type, "", now())

    internal val time: String
        get() = parse(date)

    internal enum class Type {
        SELF,
        OPPONENT,
        NOTIFICATION,
        TYPING
    }

}