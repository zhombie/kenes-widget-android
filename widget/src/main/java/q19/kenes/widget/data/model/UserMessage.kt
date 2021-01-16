package q19.kenes.widget.data.model

import androidx.annotation.Keep
import org.json.JSONObject
import org.webrtc.SessionDescription

@Keep
internal class UserMessage constructor(
    var rtc: RTC? = null,
    var action: Action? = null
) {

    @Keep
    enum class Action constructor(val value: String) {
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

@Keep
internal class RTC constructor(
    var type: Type,
    var sdp: String? = null,
    var id: String? = null,
    var label: Int? = null,
    var candidate: String? = null
) {

    @Keep
    enum class Type constructor(val value: String) {
        START("start"),
        PREPARE("prepare"),
        READY("ready"),
        OFFER("offer"),
        ANSWER("answer"),
        CANDIDATE("candidate"),
        HANGUP("hangup");

        companion object {
            fun to(type: SessionDescription.Type): Type? {
                return when (type) {
                    SessionDescription.Type.OFFER -> OFFER
                    SessionDescription.Type.ANSWER -> ANSWER
                    else -> null
                }
            }

            fun to(type: String): SessionDescription.Type? {
                return when (type) {
                    OFFER.value -> SessionDescription.Type.OFFER
                    ANSWER.value -> SessionDescription.Type.ANSWER
                    else -> null
                }
            }
        }

        override fun toString(): String {
            return value
        }
    }

    override fun toString(): String {
        return "RTC(type = $type, sdp = $sdp, id = $id, label = $label, candidate = $candidate)"
    }

}

@Keep
internal class RTCBuilder constructor() {
    var type: RTC.Type? = null
    var sdp: String? = null
    var id: String? = null
    var label: Int? = null
    var candidate: String? = null

    fun build() =
        RTC(type ?: throw IllegalStateException("Unknown RTC type"), sdp, id, label, candidate)

}


internal inline fun rtc(lambda: RTCBuilder.() -> Unit): RTC {
    return RTCBuilder().apply(lambda).build()
}
