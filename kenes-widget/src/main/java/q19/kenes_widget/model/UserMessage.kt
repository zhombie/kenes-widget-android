package q19.kenes_widget.model

import org.json.JSONObject

internal class UserMessage(
    var rtc: Rtc? = null,
    var action: Action? = null
) {

    enum class Action(val value: String) {
        FINISH("finish");

        override fun toString(): String {
            return value
        }
    }

    fun toJsonObject(): JSONObject {
        val messageObject = JSONObject()

        rtc?.let { rtc ->
            val rtcObject = JSONObject()

            try {
                rtcObject.put("type", rtc.type.value)

                if (!rtc.sdp.isNullOrBlank()) {
                    rtcObject.put("sdp", rtc.sdp)
                }

                if (!rtc.id.isNullOrBlank()) {
                    rtcObject.put("id", rtc.id)
                }

                rtc.label?.let { label ->
                    rtcObject.put("label", label)
                }

                if (!rtc.candidate.isNullOrBlank()) {
                    rtcObject.put("candidate", rtc.candidate)
                }

                messageObject.put("rtc", rtcObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        action?.let { action ->
            messageObject.put("action", action.value)
        }

        return messageObject
    }

    override fun toString(): String {
        return "UserMessage(rtc = $rtc, $action = $action)"
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
        HANGUP("hangup");

        override fun toString(): String {
            return value
        }
    }

    override fun toString(): String {
        return "Rtc(type = $type, sdp = $sdp, id = $id, label = $label, candidate = $candidate)"
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


internal inline fun rtc(lambda: RtcBuilder.() -> Unit): Rtc {
    return RtcBuilder().apply(lambda).build()
}
