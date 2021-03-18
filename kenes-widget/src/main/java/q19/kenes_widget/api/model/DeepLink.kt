package q19.kenes_widget.api.model

import java.io.Serializable

data class DeepLink constructor(
    val action: Action,
    val payload: String? = null
) : Serializable {

    enum class Action : Serializable {
        AUDIO_CALL,
        VIDEO_CALL
    }

}