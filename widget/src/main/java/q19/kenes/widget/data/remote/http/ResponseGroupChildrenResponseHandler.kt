package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.utils.json.getJSONArrayOrNull
import org.json.JSONObject
import q19.kenes.widget.domain.model.ResponseGroup

internal class ResponseGroupChildrenResponseHandler constructor(
    private val onSuccess: (children: List<ResponseGroup.Child>) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = ResponseGroupChildrenResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val json = response ?: return

        val responseGroupsJSONArray = json.getJSONArrayOrNull("response_groups")

        val children = mutableListOf<ResponseGroup.Child>()

        if (responseGroupsJSONArray != null) {
            for (i in 0 until responseGroupsJSONArray.length()) {
                val responseGroupJSONObject = responseGroupsJSONArray[i]
                if (responseGroupJSONObject is JSONObject) {
                    val responsesJSONArray = responseGroupJSONObject.getJSONArrayOrNull("responses")
                    if (responsesJSONArray != null && responsesJSONArray.length() > 0) {
                        for (k in 0 until responsesJSONArray.length()) {
                            children.add(responseGroupJSONObject.toResponseGroupChild() ?: continue)
                        }
                    }
                }
            }
        }

        onSuccess(children)
    }

    override fun onFailure(
        statusCode: Int,
        headers: Array<out Header>?,
        throwable: Throwable?,
        errorResponse: JSONObject?
    ) {
        onFailure(throwable)
    }

}