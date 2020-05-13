package q19.kenes_widget.util

import org.json.JSONArray
import org.json.JSONObject

object JsonUtil {

    fun JSONObject.optJSONArrayAsList(name: String): MutableList<Int> {
        return optJSONArray(name)?.parse() ?: mutableListOf()
    }

    private fun JSONArray?.parse(): MutableList<Int> {
        if (this == null) {
            return mutableListOf()
        }
        val list = mutableListOf<Int>()
        for (i in 0 until length()) {
            val item = this[i]
            if (item is Int) {
                list.add(item)
            }
        }
        return list
    }

    fun JSONObject.getNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key)
    }

    fun jsonObject(lambda: JSONObject.() -> Unit): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.apply(lambda)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return jsonObject
    }

}