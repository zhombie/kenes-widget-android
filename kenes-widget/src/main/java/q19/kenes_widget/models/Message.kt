package q19.kenes_widget.models

import android.text.format.DateFormat
import java.util.*

internal data class Message(
    var from_me: Boolean = false,
    var text: String,
    var date: Calendar = now()
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
        from_me: Boolean,
        text: String,
        date: Long
    ) : this(from_me, text, fromTimestamp(date))

    val time: String
        get() = parse(date)

}