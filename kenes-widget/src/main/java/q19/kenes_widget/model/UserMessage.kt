package q19.kenes_widget.model

import org.json.JSONObject

internal class UserMessage(
    var rtc: Rtc? = null
) {

    fun toJsonObject(): JSONObject {
        val messageObject = JSONObject()

        val rtcObject = JSONObject()

        try {
            rtcObject.put("type", rtc?.type?.value)

            if (!rtc?.sdp.isNullOrBlank()) {
                rtcObject.put("sdp", rtc?.sdp)
            }

            if (!rtc?.id.isNullOrBlank()) {
                rtcObject.put("id", rtc?.id)
            }

            if (rtc?.label != null) {
                rtcObject.put("label", rtc?.label)
            }

            if (!rtc?.candidate.isNullOrBlank()) {
                rtcObject.put("candidate", rtc?.candidate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return messageObject
    }

}

internal class Rtc(
    var type: Type,
    var sdp: String? = null,
    var id: String? = null,
    var label: Int? = null,
    var candidate: String? = null
) {

    enum class Type(val value: String) {
        START("start"),
        PREPARE("prepare"),
        READY("ready"),
        ANSWER("answer"),
        CANDIDATE("candidate"),
        OFFER("offer"),
        HANGUP("hangup")
    }

}

internal class RtcBuilder {
    var type: Rtc.Type? = null
    var sdp: String? = null
    var id: String? = null
    var label: Int? = null
    var candidate: String? = null

    fun build() =
        Rtc(type ?: throw IllegalStateException("Unknown RTC type"), sdp, id, label, candidate)
}

internal class UserMessageBuilder {

    private var rtc: Rtc? = null

    fun rtc(lambda: RtcBuilder.() -> Unit) {
        rtc = RtcBuilder().apply(lambda).build()
    }

    fun build() = UserMessage(rtc)

}

internal inline fun userMessage(lambda: UserMessageBuilder.() -> Unit): JSONObject {
    return UserMessageBuilder().apply(lambda).build().toJsonObject()
}
