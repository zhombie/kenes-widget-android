package q19.kenes_widget.ui.presentation.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.data.model.Category
import q19.kenes_widget.util.inflate

@Deprecated("Old way with horizontal scroll")
internal class CrossChildrenAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_CROSS_CHILD = R.layout.kenes_cell_cross_child_old
    }

    var category: Category? = null

    override fun getItemCount(): Int = category?.children?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(LAYOUT_CROSS_CHILD))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            category?.let { holder.bind(it) }
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView = view.findViewById<TextView>(R.id.textView)

        fun bind(category: Category) {
            val crossChild = category.children[absoluteAdapterPosition]

            textView?.text = crossChild.title

            textView?.setOnClickListener {
                callback.onCrossChildClicked(crossChild)
            }
        }
    }

    interface Callback {
        fun onCrossChildClicked(category: Category)
    }

}