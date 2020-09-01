package q19.kenes_widget.ui.components

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.data.model.DynamicForm
import q19.kenes_widget.data.model.DynamicFormField
import q19.kenes_widget.util.inflate

internal class DynamicFormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var titleView: TextView? = null

    private var recyclerView: RecyclerView? = null

    private var cancelButton: AppCompatButton? = null
    private var sendButton: AppCompatButton? = null

    private var adapter: DynamicFormFieldsAdapter? = null

    var dynamicForm: DynamicForm? = null
        set(value) {
            field = value
            value?.let { bindData(it) }
        }

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_dynamic_form, this)

        titleView = view.findViewById(R.id.titleView)
        recyclerView = view.findViewById(R.id.recyclerView)
        cancelButton = view.findViewById(R.id.cancelButton)
        sendButton = view.findViewById(R.id.sendButton)

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView?.layoutManager = layoutManager

        adapter = DynamicFormFieldsAdapter()
        recyclerView?.adapter = adapter

        recyclerView?.addItemDecoration(DynamicFormFieldsAdapterItemDecoration(context))

        cancelButton?.setOnClickListener { callback?.onCancelClicked() }

        sendButton?.setOnClickListener {
//            dynamicForm?.let { form ->
//                callback?.onSendClicked(form)
//            }
        }
    }

    private fun bindData(dynamicForm: DynamicForm) {
        titleView?.text = dynamicForm.title

        val fields = mutableListOf(*dynamicForm.fields.toTypedArray())
        if (dynamicForm.isFlexibleForm()) {
            fields.add(
                DynamicFormField(
                    id = 0,
                    isFlex = true,
                    title = context.getString(R.string.kenes_text),
                    type = DynamicFormField.Type.TEXT,
                    formId = dynamicForm.id,
                    level = -1,
                    value = null
                )
            )

            fields.add(
                DynamicFormField(
                    id = 1,
                    isFlex = true,
                    title = context.getString(R.string.kenes_attachment),
                    type = DynamicFormField.Type.FILE,
                    formId = dynamicForm.id,
                    level = -1,
                    value = null
                )
            )
        }

        adapter?.formFields = fields
    }

    private fun EditText?.setEmptyTextError(@StringRes resId: Int) {
        this?.error = context.getString(resId)
    }

    private fun EditText?.clearError() {
        this?.error = null
    }

    private fun EditText?.showUiError() {
        this?.isActivated = true
    }

    private fun EditText?.hideUiError() {
        this?.isActivated = false
    }

    fun resetData() {
        dynamicForm = null
    }

    fun clear() {
        titleView = null

        cancelButton = null
        sendButton = null

        adapter = null
        recyclerView?.adapter = null
        recyclerView = null

        callback = null
    }

    interface Callback {
        fun onCancelClicked()
        fun onSendClicked(dynamicForm: DynamicForm)
    }

}

private class DynamicFormFieldsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_FORM_FIELD_INPUT = R.layout.kenes_cell_form_field_input
        private val LAYOUT_FORM_FIELD_ATTACHMENT = R.layout.kenes_cell_form_field_attachment

        private const val VIEW_TYPE_INPUT = 100
        private const val VIEW_TYPE_ATTACHMENT = 101
    }

    var formFields: List<DynamicFormField> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var callback: Callback? = null

    private fun getItem(position: Int): DynamicFormField {
        return formFields[position]
    }

    override fun getItemCount(): Int = formFields.size

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.type) {
            DynamicFormField.Type.TEXT -> VIEW_TYPE_INPUT
            DynamicFormField.Type.FILE -> VIEW_TYPE_ATTACHMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_INPUT ->
                InputViewHolder(parent.inflate(LAYOUT_FORM_FIELD_INPUT))
            VIEW_TYPE_ATTACHMENT ->
                AttachmentViewHolder(parent.inflate(LAYOUT_FORM_FIELD_ATTACHMENT))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is InputViewHolder) {
            holder.bind(formFields[position])
        } else if (holder is AttachmentViewHolder) {
            holder.bind(formFields[position])
        }
    }

    private inner class InputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val editText = view.findViewById<EditText>(R.id.editText)

        fun bind(field: DynamicFormField) {
            labelView.text = field.title
            editText.hint = field.title
        }
    }

    private inner class AttachmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val selectAttachmentButton = view.findViewById<AppCompatButton>(R.id.selectAttachmentButton)

        fun bind(field: DynamicFormField) {
            labelView.text = field.title

            selectAttachmentButton.setOnClickListener {
                callback?.onSelectAttachmentButtonClicked(field)
            }
        }
    }

    interface Callback {
        fun onSelectAttachmentButtonClicked(field: DynamicFormField)
    }

}


private class DynamicFormFieldsAdapterItemDecoration(
    context: Context
) : RecyclerView.ItemDecoration() {

    private var verticalSpacing: Int =
        context.resources.getDimensionPixelOffset(R.dimen.kenes_form_field_vertical_spacing)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.bottom = verticalSpacing
    }
}