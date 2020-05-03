package q19.kenes_widget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.Response

internal class SectionChildrenAdapter(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_SECTION_CHILD = R.layout.kenes_cell_small_section
    }

    var response: Response? = null

    override fun getItemCount(): Int = response?.responses?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(LAYOUT_SECTION_CHILD, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            response?.let { holder.bind(it) }
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView: TextView? = null

        init {
            textView = view.findViewById(R.id.textView)
        }

        fun bind(response: Response) {
            val section = response.responses[adapterPosition]

            textView?.text = section.title

            textView?.setOnClickListener {
                callback.onSectionClicked(section)
            }
        }
    }

    interface Callback {
        fun onSectionClicked(response: Response)
    }

}