package q19.kenes.widget.ui.presentation.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

class ResponseGroupChildrenHeaderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ResponseGroupChildrenHeaderAdapter::class.java.simpleName
    }

    var title: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.cell_response_group_header))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind()
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)

        fun bind() {
            titleView.text = title
        }

    }

}