package q19.kenes_widget.data.model

internal data class DynamicForm constructor(
    val id: Long,
    val title: String? = null,
    val isFlex: Int = 0,
    val prompt: String? = null,
    var fields: List<DynamicFormField> = emptyList()
) {

    fun isFlexibleForm(): Boolean {
        return isFlex == 1
    }

}

internal data class DynamicFormField constructor(
    val id: Long,
    val isFlex: Boolean = false,
    val title: String? = null,
    val prompt: String? = null,
    val type: Type,
    val default: String? = null,
    val formId: Long,
    val configs: Configs? = null,
    val level: Int,
    var value: String? = null
) {

    enum class Type(val value: String) {
        TEXT("text"),
        FILE("file")
    }

    class Configs

}