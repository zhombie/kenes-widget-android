package q19.kenes_widget.ui.components

import android.content.Context
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
import q19.kenes_widget.model.DynamicForm
import q19.kenes_widget.model.DynamicFormField
import q19.kenes_widget.util.inflate

class DynamicFormView @JvmOverloads constructor(
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

    var form: DynamicForm? = null
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

        cancelButton?.setOnClickListener { callback?.onCancelClicked() }

        sendButton?.setOnClickListener {
//            callback?.onSendClicked()
        }
    }

    private fun bindData(form: DynamicForm) {
        titleView?.text = form.title
        adapter?.fields = form.fields
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
        fun onSendClicked(data: DynamicForm)
    }

}

private class DynamicFormFieldsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_FORM_FIELD = R.layout.kenes_cell_form_field
    }

    var fields: List<DynamicFormField> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = fields.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(LAYOUT_FORM_FIELD))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) holder.bind(fields[position])
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val labelView = view.findViewById<TextView>(R.id.labelView)
        private val editText = view.findViewById<EditText>(R.id.editText)

        fun bind(field: DynamicFormField) {
            labelView.text = field.title
            editText.hint = field.title
        }
    }

}