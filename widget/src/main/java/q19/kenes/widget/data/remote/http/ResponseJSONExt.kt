package q19.kenes.widget.data.remote.http

import kz.q19.domain.model.language.Language
import kz.q19.utils.json.getIntOrNull
import kz.q19.utils.json.getJSONArrayOrNull
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getStringOrNull
import org.json.JSONObject
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.domain.model.ResponseInfo

fun JSONObject.toResponseGroup(isPrimary: Boolean, children: MutableList<Nestable>): ResponseGroup? {
    return ResponseGroup(
        id = getLongOrNull("id") ?: return null,
        title = getStringOrNull("title") ?: "",
        language = when (getIntOrNull("lang")) {
            Language.ID.KK.value.toInt() -> Language.KAZAKH
            Language.ID.RU.value.toInt() -> Language.RUSSIAN
            Language.ID.EN.value.toInt() -> Language.ENGLISH
            else -> return null
        },
        isPrimary = isPrimary,
        children = children
    )
}

fun JSONObject.toResponseGroupChild(): ResponseGroup.Child? {
    val responses = mutableListOf<Long>()

    val responsesJSONArray = getJSONArrayOrNull("responses")

    if (responsesJSONArray != null) {
        for (i in 0 until responsesJSONArray.length()) {
            val responseId = responsesJSONArray[i]
            if (responseId is Long) {
                responses.add(responseId)
            } else if (responseId is Int) {
                responses.add(responseId.toLong())
            } else {
                continue
            }
        }
    }

    return ResponseGroup.Child(
        id = getLongOrNull("id") ?: return null,
        title = getStringOrNull("title") ?: "",
        language = when (getIntOrNull("lang")) {
            Language.ID.KK.value.toInt() -> Language.KAZAKH
            Language.ID.RU.value.toInt() -> Language.RUSSIAN
            Language.ID.EN.value.toInt() -> Language.ENGLISH
            else -> return null
        },
        responses = responses.map { ResponseInfo(id = it) }
    )
}