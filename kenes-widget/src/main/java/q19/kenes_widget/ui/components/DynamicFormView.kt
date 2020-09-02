package q19.kenes_widget.ui.components

import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.data.model.Attachment
import q19.kenes_widget.data.model.DynamicForm
import q19.kenes_widget.data.model.DynamicFormField
import q19.kenes_widget.ui.components.base.TitleView
import q19.kenes_widget.util.*

internal class DynamicFormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val TAG = "DynamicFormView"
    }

//    private var titleView: TitleView? = null

    private var recyclerView: RecyclerView? = null

//    private var cancelButton: AppCompatButton? = null
//    private var sendButton: AppCompatButton? = null

    private var adapter: DynamicFormFieldsAdapter? = null

    var dynamicForm: DynamicForm? = null
        set(value) {
            field = value
            value?.let { bindData(it) }
        }

    var attachment: Attachment? = null
        set(value) {
            field = value
            value?.let { bindAttachment(it) }
        }

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_dynamic_form, this)

//        titleView = view.findViewById(R.id.titleView)
        recyclerView = view.findViewById(R.id.recyclerView)
//        cancelButton = view.findViewById(R.id.cancelButton)
//        sendButton = view.findViewById(R.id.sendButton)

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView?.layoutManager = layoutManager

        adapter = DynamicFormFieldsAdapter()
        adapter?.callback = object : DynamicFormFieldsAdapter.Callback {
            override fun onSendButtonClicked() {
                val fields = adapter?.formFields
                Logger.debug(TAG, "fields: $fields")

                if (!fields.isNullOrEmpty()) {
                    val emptyFields = fields.filter { !it.isFlex && it.value.isNullOrBlank() }
                    if (emptyFields.isNullOrEmpty()) {
                        dynamicForm?.fields = fields
                        dynamicForm?.let { form -> callback?.onSendButtonClicked(form) }
                    } else {
                        val text = emptyFields.map { it.title }.joinToString(separator = "\n")
                        context.AlertDialogBuilder
                            .setCancelable(true)
                            .setTitle(R.string.kenes_attention)
                            .setMessage(context.getString(R.string.kenes_fill_fields, text))
                            .setPositiveButton(R.string.kenes_ok) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            }

            override fun onCancelButtonClicked() {
                callback?.onCancelButtonClicked()
            }

            override fun onSelectAttachmentButtonClicked(field: DynamicFormField) {
                callback?.onSelectAttachmentButtonClicked(field)
            }

            override fun onAttachmentClicked(attachment: Attachment) {
                callback?.onAttachmentClicked(attachment)
            }
        }

        recyclerView?.adapter = adapter

        recyclerView?.setHasFixedSize(true)

        recyclerView?.disableChangeAnimations()

        recyclerView?.addItemDecoration(DynamicFormFieldsAdapterItemDecoration(context))

//        cancelButton?.setOnClickListener { callback?.onCancelButtonClicked() }

//        sendButton?.setOnClickListener {}
    }

    private fun bindData(dynamicForm: DynamicForm) {
//        if (dynamicForm.title.isNullOrBlank()) {
//            titleView?.visibility = View.GONE
//        } else {
//            titleView?.text = dynamicForm.title
//            titleView?.visibility = View.VISIBLE
//        }

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

        this.dynamicForm?.fields = fields
        adapter?.dynamicForm = this.dynamicForm
    }

    private fun bindAttachment(attachment: Attachment) {
        adapter?.attachment = attachment
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
        adapter?.dynamicForm = null
        adapter?.formFields = emptyList()
        adapter?.attachment = null
    }

    fun clear() {
//        titleView = null

//        cancelButton = null
//        sendButton = null

        adapter = null
        recyclerView?.adapter = null
        recyclerView = null

        callback = null
    }

    interface Callback {
        fun onCancelButtonClicked()
        fun onSendButtonClicked(dynamicForm: DynamicForm)
        fun onSelectAttachmentButtonClicked(field: DynamicFormField)
        fun onAttachmentClicked(attachment: Attachment)
    }

}

private class DynamicFormFieldsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_FORM_TITLE = R.layout.kenes_cell_form_title
        private val LAYOUT_FORM_FIELD_INPUT = R.layout.kenes_cell_form_field_input
        private val LAYOUT_FORM_FIELD_ATTACHMENT = R.layout.kenes_cell_form_field_attachment
        private val LAYOUT_FORM_FOOTER = R.layout.kenes_cell_form_footer

        private const val VIEW_TYPE_TITLE = 100
        private const val VIEW_TYPE_INPUT = 101
        private const val VIEW_TYPE_ATTACHMENT = 102
        private const val VIEW_TYPE_FOOTER = 103
    }

    var dynamicForm: DynamicForm? = null
        set(value) {
            field = value
            this.formFields = field?.fields ?: emptyList()
        }

    var attachment: Attachment? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var formFields: List<DynamicFormField> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var callback: Callback? = null

    private fun getItem(position: Int): DynamicFormField? {
        val relativePosition = position - 1
        if (relativePosition < 0 || relativePosition >= formFields.size) {
            return null
        }
        return formFields[relativePosition]
    }

    override fun getItemCount(): Int = formFields.size + 1 + 1

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_TITLE
            itemCount - 1 -> VIEW_TYPE_FOOTER
            else -> {
                val item = getItem(position)
                when (item?.type) {
                    DynamicFormField.Type.TEXT -> VIEW_TYPE_INPUT
                    DynamicFormField.Type.FILE -> VIEW_TYPE_ATTACHMENT
                    else -> super.getItemViewType(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TITLE ->
                TitleViewHolder(parent.inflate(LAYOUT_FORM_TITLE))
            VIEW_TYPE_INPUT ->
                InputViewHolder(parent.inflate(LAYOUT_FORM_FIELD_INPUT))
            VIEW_TYPE_ATTACHMENT ->
                AttachmentViewHolder(parent.inflate(LAYOUT_FORM_FIELD_ATTACHMENT))
            VIEW_TYPE_FOOTER ->
                FooterViewHolder(parent.inflate(LAYOUT_FORM_FOOTER))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TitleViewHolder -> holder.bind((dynamicForm?.title ?: "").trim())
            is InputViewHolder -> item?.let { holder.bind(it) }
            is AttachmentViewHolder -> item?.let { holder.bind(it) }
            is FooterViewHolder -> holder.bind()
        }
    }

    private class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<TitleView>(R.id.titleView)

        fun bind(title: String) {
            titleView?.text = title
        }
    }

    private inner class InputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val infoView = view.findViewById<TextView>(R.id.infoView)
        private val editText = view.findViewById<EditText>(R.id.editText)

        fun bind(field: DynamicFormField) {
            labelView.text = field.title?.trim()

            if (field.prompt.isNullOrBlank()) {
                infoView?.visibility = View.GONE
            } else {
                infoView?.text = field.prompt.trim()
                infoView?.visibility = View.VISIBLE
            }

            editText?.hint = field.title?.trim()

            editText?.addTextChangedListener(object : KenesTextWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString()?.trim()
                    field.value = text
                }
            })
        }
    }

    private inner class AttachmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val selectAttachmentButton = view.findViewById<AppCompatButton>(R.id.selectAttachmentButton)
        private val attachmentView = view.findViewById<AppCompatTextView>(R.id.attachmentView)

        fun bind(field: DynamicFormField) {
            labelView?.text = field.title

            if (attachment != null) {
                field.value = attachment?.url

                attachmentView?.text = attachment?.title

                attachmentView?.setOnClickListener {
                    callback?.onAttachmentClicked(attachment!!)
                }

                selectAttachmentButton?.visibility = View.GONE
                attachmentView?.visibility = View.VISIBLE
            } else {
                field.value = null

                attachmentView?.setOnClickListener(null)

                attachmentView?.visibility = View.GONE
                selectAttachmentButton?.visibility = View.VISIBLE
            }

            selectAttachmentButton?.setOnClickListener {
                callback?.onSelectAttachmentButtonClicked(field)
            }
        }
    }

    private inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cancelButton = view.findViewById<AppCompatButton>(R.id.cancelButton)
        private val sendButton = view.findViewById<AppCompatButton>(R.id.sendButton)

        fun bind() {
            cancelButton?.setOnClickListener {
                callback?.onCancelButtonClicked()
            }

            sendButton?.setOnClickListener {
                callback?.onSendButtonClicked()
            }
        }
    }

    interface Callback {
        fun onSelectAttachmentButtonClicked(field: DynamicFormField)
        fun onCancelButtonClicked()
        fun onSendButtonClicked()
        fun onAttachmentClicked(attachment: Attachment)
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