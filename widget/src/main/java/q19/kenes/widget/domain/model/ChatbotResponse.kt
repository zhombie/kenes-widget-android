package q19.kenes.widget.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kz.q19.domain.model.language.Language
import kz.q19.domain.model.media.Media

@Keep
@Parcelize
open class Element : Parcelable


@Keep
@Parcelize
open class Nestable constructor(
    open val id: Long,
    open val title: String,
    open val language: Language
) : Element(), Parcelable


@Keep
@Parcelize
data class ResponseGroup constructor(
    override val id: Long,
    override val title: String,
    override val language: Language,
    val isPrimary: Boolean,
    val children: List<Nestable>
) : Nestable(id = id, title = title, language = language), Parcelable {

    @Keep
    @Parcelize
    data class Child constructor(
        override val id: Long,
        override val title: String,
        override val language: Language,
        val responses: List<Response>  // Collection of response ids
    ) : Nestable(id = id, title = title, language = language), Parcelable

}


@Parcelize
@Keep
data class Response constructor(
    val id: Long,
    val messageId: String? = null,
    val text: String? = null,
    val time: Long = -1L,
    val attachments: List<Media> = emptyList(),
    val form: Form? = null
) : Element(), Parcelable {

    @Parcelize
    @Keep
    data class Form constructor(
        val id: Long,
        val title: String,
        val prompt: String? = null
    ) : Parcelable

}