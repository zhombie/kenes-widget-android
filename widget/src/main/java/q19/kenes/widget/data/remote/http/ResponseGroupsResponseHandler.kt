package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.domain.model.language.Language
import kz.q19.utils.json.getIntOrNull
import kz.q19.utils.json.getJSONArrayOrNull
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getStringOrNull
import org.json.JSONObject
import q19.kenes.widget.domain.model.AnyResponse
import q19.kenes.widget.domain.model.ResponseGroup

internal class ResponseGroupsResponseHandler constructor(
    private val onSuccess: (responseGroups: List<ResponseGroup>) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = ResponseGroupsResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val json = response ?: return

        val responseGroupsJSONArray = json.getJSONArrayOrNull("response_groups")

        val responseGroups = mutableListOf<ResponseGroup>()

        if (responseGroupsJSONArray != null) {
            for (i in 0 until responseGroupsJSONArray.length()) {
                val responseGroupJSONObject = responseGroupsJSONArray[i]
                if (responseGroupJSONObject is JSONObject) {
                    val children = mutableListOf<AnyResponse>()

                    val childrenJSONArray = responseGroupJSONObject.getJSONArrayOrNull("children")
                    if (childrenJSONArray != null) {
                        for (j in 0 until childrenJSONArray.length()) {
                            val childJSONObject = childrenJSONArray[j]
                            if (childJSONObject is JSONObject) {
                                val responses = childJSONObject.getJSONArrayOrNull("responses")
                                if (responses == null || responses.length() == 0) {
                                    children.add(childJSONObject.toResponseGroup(mutableListOf()) ?: continue)
                                } else {
                                    children.add(childJSONObject.toResponseGroupChild() ?: continue)
                                }
                            }
                        }
                    }

                    responseGroups.add(responseGroupJSONObject.toResponseGroup(children) ?: continue)
                }
            }
        }

        onSuccess(responseGroups)
    }

    private fun JSONObject.toResponseGroup(children: MutableList<AnyResponse>): ResponseGroup? {
        return ResponseGroup(
            id = getLongOrNull("id") ?: return null,
            title = getStringOrNull("title") ?: "",
            language = when (getIntOrNull("lang")) {
                Language.ID.KK.value.toInt() -> Language.KAZAKH
                Language.ID.RU.value.toInt() -> Language.RUSSIAN
                Language.ID.EN.value.toInt() -> Language.ENGLISH
                else -> return null
            },
            children = children
        )
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONObject?
    ) {
        onFailure(throwable)
    }

    private fun JSONObject.toResponseGroupChild(): ResponseGroup.Child? {
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
            responses = responses
        )
    }

}