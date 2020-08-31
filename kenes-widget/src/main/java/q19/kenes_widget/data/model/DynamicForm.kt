package q19.kenes_widget.data.model

internal data class DynamicForm(
    val id: Long,
    val title: String? = null,
    val isFlex: Int = 0,
    val prompt: String? = null,
    val fields: List<DynamicFormField>
)

internal class DynamicFormField(
    val id: Long,
    val title: String? = null,
    val prompt: String? = null,
    val type: String,
    val default: String? = null,
    val formId: Long,
    val level: Int
)