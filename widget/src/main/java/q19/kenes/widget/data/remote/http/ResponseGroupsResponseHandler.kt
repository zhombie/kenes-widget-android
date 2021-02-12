package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.domain.model.knowledge_base.BaseResponse
import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.language.Language
import kz.q19.utils.json.getArrayOrNull
import kz.q19.utils.json.getIntOrNull
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getStringOrNull
import org.json.JSONObject

internal class ResponseGroupsResponseHandler constructor(
    private val onSuccess: (responseGroups: List<ResponseGroup>) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = ResponseGroupsResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val json = response ?: return

        val responseGroupsJSONArray = json.getArrayOrNull("response_groups")

        val responseGroups = mutableListOf<ResponseGroup>()

        if (responseGroupsJSONArray != null) {
            for (i in 0 until responseGroupsJSONArray.length()) {
                val responseGroupJSONObject = responseGroupsJSONArray[i]
                if (responseGroupJSONObject is JSONObject) {
                    val children = mutableListOf<BaseResponse>()

                    val childrenJSONArray = responseGroupJSONObject.getArrayOrNull("children")
                    if (childrenJSONArray != null) {
                        for (j in 0 until childrenJSONArray.length()) {
                            val childJSONObject = childrenJSONArray[j]
                            if (childJSONObject is JSONObject) {
                                val responses = childJSONObject.getArrayOrNull("responses")
                                if (responses == null || responses.length() == 0) {
                                    children.add(childJSONObject.toResponseGroup(mutableListOf()) ?: continue)
                                } else {
                                    children.add(childJSONObject.toResponse() ?: continue)
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

    private fun JSONObject.toResponseGroup(children: MutableList<BaseResponse>): ResponseGroup? {
        return ResponseGroup(
            id = getLongOrNull("id") ?: return null,
            title = getStringOrNull("title") ?: "",
            language = when (getIntOrNull("lang")) {
                Language.ID.KK.value.toInt() -> Language.KAZAKH
                Language.ID.RU.value.toInt() -> Language.RUSSIAN
                Language.ID.EN.value.toInt() -> Language.ENGLISH
                else -> return null
            },
            children = children,
            extra = ResponseGroup.Extra(getIntOrNull("order"))
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

    private fun JSONObject.toResponse(): Response? {
        val responses = mutableListOf<Long>()

        val responsesJSONArray = getArrayOrNull("responses")

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

        return Response(
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