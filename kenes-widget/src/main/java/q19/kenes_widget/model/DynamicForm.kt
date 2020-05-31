package q19.kenes_widget.model

data class DynamicForm(
    val id: Long,
    val title: String? = null,
    val isFlex: Int = 0,
    val prompt: String? = null,
    val fields: List<DynamicFormField>
)

class DynamicFormField(
    val id: Long,
    val title: String? = null,
    val prompt: String? = null,
    val type: String,
    val default: String? = null,
    val formId: Long,
    val level: Int
)