package q19.kenes.widget.data.model

import androidx.annotation.Keep

@Keep
internal data class WidgetIceServer constructor(
    val url: String?,
    val username: String? = null,
    val urls: String?,
    val credential: String? = null
)