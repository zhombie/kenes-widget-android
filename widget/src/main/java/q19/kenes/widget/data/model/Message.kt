package q19.kenes.widget.data.model

import android.text.Spanned
import android.text.format.DateFormat
import androidx.annotation.Keep
import q19.kenes.widget.util.html.HtmlCompat
import java.util.*

@Keep
internal data class Message constructor(
    var id: String? = null,
    var type: Type = Type.INCOMING,
    var text: String,
    var replyMarkup: ReplyMarkup? = null,
    var media: Media? = null,
    var attachments: List<Attachment>? = null,
    var date: Calendar = now(),
    var category: Category? = null,
    var dynamicForm: DynamicForm? = null,

    // Local variables
    val file: File = File()
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
        replyMarkup: ReplyMarkup? = null,
        media: Media? = null,
        attachments: List<Attachment>? = null,
        timestamp: Long? = null,
        category: Category? = null
    ) : this(
        id = null,
        type = type,
        text = text?.trim() ?: "",
        replyMarkup = replyMarkup,
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
                HtmlCompat.fromHtml(text)
            } else null
        }

    @Keep
    data class ReplyMarkup constructor(
        val rows: List<List<Button>> = emptyList()
    ) {

        @Keep
        data class Button constructor(
            val text: String,
            val callbackData: String? = null,
            val url: String? = null
        )

        fun getAllButtons(): MutableList<Button> {
            val buttons = mutableListOf<Button>()
            rows.forEach {
                it.forEach { button ->
                    buttons.add(button)
                }
            }
            return buttons
        }

        fun getColumnsCount(): Int {
            return if (rows.isNullOrEmpty()) {
                0
            } else {
                rows.first().size
            }
        }

    }

    @Keep
    data class File constructor(
        var type: String? = null
    ) {

        @Keep
        enum class DownloadStatus {
            NONE,
            PENDING,
            ERROR,
            COMPLETED
        }

        var progress: Int = 0
            set(value) {
                field = value
                if (value == 100) {
                    downloadStatus = DownloadStatus.COMPLETED
                } else if (value in 1..99) {
                    downloadStatus = DownloadStatus.PENDING
                }
            }

        var downloadStatus: DownloadStatus = DownloadStatus.NONE
            set(value) {
                if (value == DownloadStatus.NONE) {
                    field = value
                    return
                } else if (field == DownloadStatus.COMPLETED) {
                    return
                }
                if (field == value) return
                field = value
            }

    }

    @Keep
    enum class Type {
        OUTGOING,
        INCOMING,

        NOTIFICATION,

        TYPING,

        CATEGORY,
        CROSS_CHILDREN,
        RESPONSE
    }

}