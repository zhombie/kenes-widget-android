package q19.kenes.widget.ui.presentation.call.selection

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kz.q19.domain.model.call.Call

@Parcelize
internal data class CallSelection constructor(
    val id: Long,
    val title: String,
    val calls: List<Call>
) : Parcelable