package q19.kenes.widget.data.remote.http

import com.loopj.android.http.JsonHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kz.q19.domain.model.media.Media
import kz.q19.utils.enums.findEnumBy
import kz.q19.utils.json.getJSONArrayOrNull
import kz.q19.utils.json.getJSONObjectOrNull
import kz.q19.utils.json.getLongOrNull
import kz.q19.utils.json.getStringOrNull
import org.json.JSONObject
import q19.kenes.widget.domain.model.ResponseInfo

internal class ResponseInfoResponseHandler constructor(
    private val responseId: Long,
    private val onSuccess: (responseInfo: ResponseInfo) -> Unit,
    private val onFailure: (throwable: Throwable?) -> Unit
) : JsonHttpResponseHandler() {

    companion object {
        private val TAG = ResponseInfoResponseHandler::class.java.simpleName
    }

    override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
        val json = response ?: return

        val responseJSONObject = json.getJSONObjectOrNull("response")

        if (responseJSONObject == null) {
            onFailure(NullPointerException())
        } else {
            val attachments = mutableListOf<Media>()
            val attachmentsJSONArray = responseJSONObject.getJSONArrayOrNull("attachments")
            if (attachmentsJSONArray != null) {
                for (i in 0 until attachmentsJSONArray.length()) {
                    val attachmentJSONObject = attachmentsJSONArray[i]
                    if (attachmentJSONObject is JSONObject) {
                        attachments.add(
                            Media(
                                id = "Unknown",
                                type = findEnumBy { it.key == attachmentJSONObject.getStringOrNull("type") },
                                extension = findEnumBy {
                                    it.value == attachmentJSONObject.getStringOrNull(
                                        "ext"
                                    )
                                },
                                urlPath = attachmentJSONObject.getStringOrNull("url"),
                            )
                        )
                    }
                }
            }

            var form: ResponseInfo.Form? = null
            val formJSONObject = responseJSONObject.getJSONObjectOrNull("form")
            if (formJSONObject != null) {
                form = ResponseInfo.Form(
                    id = formJSONObject.getLong("id"),
                    title = formJSONObject.getStringOrNull("title") ?: "",
                    prompt = formJSONObject.getStringOrNull("prompt")
                )
            }

            onSuccess(
                ResponseInfo(
                    id = responseId,
                    messageId = responseJSONObject.getString("id"),
                    text = responseJSONObject.getStringOrNull("text") ?: "",
                    time = responseJSONObject.getLongOrNull("time") ?: -1L,
                    attachments = attachments,
                    form = form
                )
            )
        }
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