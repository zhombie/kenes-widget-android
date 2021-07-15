package q19.kenes.widget.domain.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kz.q19.domain.model.language.Language

@Keep
@Parcelize
open class AnyResponse constructor(
    open val id: Long,
    open val title: String,
    open val language: Language
) : Parcelable


@Keep
@Parcelize
data class ResponseGroup constructor(
    override val id: Long,
    override val title: String,
    override val language: Language,
    val children: MutableList<AnyResponse>
) : AnyResponse(id, title, language), Parcelable {

    @Keep
    @Parcelize
    data class Child constructor(
        override val id: Long,
        override val title: String,
        override val language: Language,
        val responses: List<Long>  // Collection of response ids
    ) : AnyResponse(id, title, language), Parcelable

}