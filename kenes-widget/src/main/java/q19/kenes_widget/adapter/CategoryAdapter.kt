package q19.kenes_widget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.Category

internal class CategoryAdapter(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_CATEGORY = R.layout.kenes_cell_category_child
    }

    var category: Category? = null

    override fun getItemCount(): Int = category?.children?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(LAYOUT_CATEGORY, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            category?.let { holder.bind(it) }
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView: TextView? = null

        init {
            textView = view.findViewById(R.id.textView)
        }

        fun bind(category: Category) {
            val child = category.children[adapterPosition]

            textView?.text = child.title

            textView?.setTextColor(category.color)

            textView?.background = category.getDefaultBackground(itemView.resources)

            textView?.setOnClickListener {
                callback.onChildClicked(child)
            }
        }
    }

    interface Callback {
        fun onChildClicked(category: Category)
    }

}