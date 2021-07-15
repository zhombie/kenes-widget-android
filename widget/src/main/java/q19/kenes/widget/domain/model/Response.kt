package q19.kenes.widget.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kz.q19.domain.model.media.Media

@Parcelize
@Keep
data class Response constructor(
    val id: String,
    val text: String,
    val time: Long,
    val attachments: List<Media> = emptyList(),
    val form: Form? = null
) : Parcelable {

    @Parcelize
    @Keep
    data class Form constructor(
        val id: Long,
        val title: String,
        val prompt: String? = null
    ) : Parcelable

}