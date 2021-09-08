package q19.kenes.widget.ui.components.deprecated

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
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.domain.model.form.Form
import kz.q19.domain.model.media.Media
import kz.q19.utils.recyclerview.disableChangeAnimations
import kz.q19.utils.textview.AbstractTextWatcher
import kz.q19.utils.view.inflate
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.deprecated.base.KenesTitleView
import q19.kenes.widget.util.AlertDialogBuilder
import q19.kenes_widget.R

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

    private var adapter: FieldsAdapter? = null

    var form: Form? = null
        set(value) {
            field = value
            bindData(form)
        }

    var attachment: Media? = null
        set(value) {
            field = value
            bindAttachment(attachment)
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

        adapter = FieldsAdapter()
        adapter?.callback = object : FieldsAdapter.Callback {
            override fun onSendButtonClicked() {
                val fields = adapter?.formFields
                Logger.debug(TAG, "fields: $fields")

                if (!fields.isNullOrEmpty()) {
                    val emptyFields = fields.filter { !it.isFlexible && it.value.isNullOrBlank() }
                    if (emptyFields.isNullOrEmpty()) {
//                        form?.fields = fields
                        form?.let { form -> callback?.onSendButtonClicked(form) }
                    } else {
                        val text = emptyFields.joinToString(separator = "\n") { it.title }
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

            override fun onSelectAttachmentButtonClicked(field: Form.Field) {
                callback?.onSelectAttachmentButtonClicked(field)
            }

            override fun onAttachmentClicked(attachment: Media) {
                callback?.onAttachmentClicked(attachment)
            }
        }

        recyclerView?.adapter = adapter

        recyclerView?.setHasFixedSize(true)

        recyclerView?.disableChangeAnimations()

        recyclerView?.addItemDecoration(FieldsAdapterItemDecoration(context))

//        cancelButton?.setOnClickListener { callback?.onCancelButtonClicked() }

//        sendButton?.setOnClickListener {}
    }

    private fun bindData(form: Form?) {
        Logger.debug(TAG, "form: $form")
        if (form == null) {
            adapter?.form = null
            adapter?.attachment = null
            adapter?.notifyDataSetChanged()
        } else {
//        if (form.title.isNullOrBlank()) {
//            titleView?.visibility = View.GONE
//        } else {
//            titleView?.text = form.title
//            titleView?.visibility = View.VISIBLE
//        }

            val fields = mutableListOf(*form.fields.toTypedArray())
            if (form.isFlexible) {
                fields.add(
                    Form.Field(
                        id = 0,
                        isFlexible = true,
                        title = context.getString(R.string.kenes_text),
                        type = Form.Field.Type.TEXT,
                        level = -1,
                        value = null
                    )
                )

                fields.add(
                    Form.Field(
                        id = 1,
                        isFlexible = true,
                        title = context.getString(R.string.kenes_attachment),
                        type = Form.Field.Type.FILE,
                        level = -1,
                        value = null
                    )
                )
            }

//            this.form?.fields = fields
            adapter?.form = this.form
        }
    }

    private fun bindAttachment(attachment: Media?) {
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
        form = null
        attachment = null
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
        fun onSendButtonClicked(form: Form)
        fun onSelectAttachmentButtonClicked(field: Form.Field)
        fun onAttachmentClicked(attachment: Media)
    }

}

private class FieldsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    var form: Form? = null
        set(value) {
            field = value
            this.formFields = value?.fields ?: emptyList()
        }

    var attachment: Media? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var formFields: List<Form.Field> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var callback: Callback? = null

    private fun getItem(position: Int): Form.Field? {
        val relativePosition = position - 1
        if (relativePosition < 0 || relativePosition >= formFields.size) {
            return null
        }
        return formFields[relativePosition]
    }

    override fun getItemCount(): Int = if (formFields.isEmpty()) 0 else formFields.size + 1 + 1

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_TITLE
            itemCount - 1 -> VIEW_TYPE_FOOTER
            else -> {
                val item = getItem(position)
                when (item?.type) {
                    Form.Field.Type.TEXT -> VIEW_TYPE_INPUT
                    Form.Field.Type.FILE -> VIEW_TYPE_ATTACHMENT
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
            is TitleViewHolder -> holder.bind((form?.title ?: "").trim())
            is InputViewHolder -> holder.bind(item)
            is AttachmentViewHolder -> holder.bind(item)
            is FooterViewHolder -> holder.bind()
        }
    }

    private class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<KenesTitleView>(R.id.titleView)

        fun bind(title: String) {
            titleView?.text = title
        }
    }

    private inner class InputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val infoView = view.findViewById<TextView>(R.id.infoView)
        private val editText = view.findViewById<EditText>(R.id.editText)

        fun bind(field: Form.Field?) {
            if (field == null) {
                labelView?.text = null
                infoView?.text = null
                editText?.text = null
            } else {
                labelView?.text = field.title.trim()

                if (field.prompt.isNullOrBlank()) {
                    infoView?.visibility = View.GONE
                } else {
//                    infoView?.text = field.prompt.trim()
                    infoView?.visibility = View.VISIBLE
                }

                editText?.setText(field.value)

                editText?.hint = field.title.trim()

                editText?.addTextChangedListener(object : AbstractTextWatcher() {
                    override fun afterTextChanged(s: Editable?) {
                        val text = s?.toString()?.trim()
//                        field.value = text
                    }
                })
            }
        }
    }

    private inner class AttachmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val selectAttachmentButton = view.findViewById<AppCompatButton>(R.id.selectAttachmentButton)
        private val attachmentView = view.findViewById<AppCompatTextView>(R.id.attachmentView)

        fun bind(field: Form.Field?) {
            if (field == null) {
                labelView?.text = null

                attachmentView?.text = null
                attachmentView?.setOnClickListener(null)

                attachmentView?.visibility = View.GONE
                selectAttachmentButton?.visibility = View.VISIBLE
            } else {
                labelView?.text = field.title

                val attachment = attachment
                if (attachment != null) {
//                    field.value = attachment.url

                    attachmentView?.text = attachment.title

                    attachmentView?.setOnClickListener {
                        callback?.onAttachmentClicked(attachment)
                    }

                    selectAttachmentButton?.visibility = View.GONE
                    attachmentView?.visibility = View.VISIBLE
                } else {
//                    field.value = null

                    attachmentView?.setOnClickListener(null)

                    attachmentView?.visibility = View.GONE
                    selectAttachmentButton?.visibility = View.VISIBLE
                }

                selectAttachmentButton?.setOnClickListener {
                    callback?.onSelectAttachmentButtonClicked(field)
                }
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
        fun onSelectAttachmentButtonClicked(field: Form.Field)
        fun onCancelButtonClicked()
        fun onSendButtonClicked()
        fun onAttachmentClicked(attachment: Media)
    }

}


private class FieldsAdapterItemDecoration(
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