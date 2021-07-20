package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.utils.json.getJSONArrayOrNull
import org.json.JSONObject
import q19.kenes.widget.domain.model.Nestable
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

        if (responseGroupsJSONArray != null && responseGroupsJSONArray.length() > 0) {
            for (i in 0 until responseGroupsJSONArray.length()) {
                val responseGroupJSONObject = responseGroupsJSONArray[i]
                if (responseGroupJSONObject is JSONObject) {
                    val nestables = mutableListOf<Nestable>()

                    val childrenJSONArray = responseGroupJSONObject.getJSONArrayOrNull("children")
                    if (childrenJSONArray != null && childrenJSONArray.length() > 0) {
                        for (j in 0 until childrenJSONArray.length()) {
                            val childJSONObject = childrenJSONArray[j]
                            if (childJSONObject is JSONObject) {
                                val responses = childJSONObject.getJSONArrayOrNull("responses")
                                if (responses == null || responses.length() == 0) {
                                    nestables.add(
                                        childJSONObject.toResponseGroup(
                                            isPrimary = false,
                                            children = mutableListOf()
                                        ) ?: continue
                                    )
                                } else {
                                    nestables.add(childJSONObject.toResponseGroupChild() ?: continue)
                                }
                            }
                        }
                    }

                    responseGroups.add(
                        responseGroupJSONObject.toResponseGroup(
                            isPrimary = true,
                            children = nestables
                        ) ?: continue
                    )
                }
            }
        }

        onSuccess(responseGroups)
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