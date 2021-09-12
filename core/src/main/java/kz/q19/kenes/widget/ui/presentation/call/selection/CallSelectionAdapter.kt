package kz.q19.kenes.widget.ui.presentation.call.selection

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.domain.model.call.Call
import kz.q19.utils.view.inflate
import kz.q19.kenes.widget.R

internal class CallSelectionAdapter constructor(
    private val callback: (call: Call) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = CallSelectionAdapter::class.java.simpleName
    }

    var calls: List<Call> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): Call {
        return calls[position]
    }

    override fun getItemCount(): Int = calls.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.kenes_cell_call_selection))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<ShapeableImageView>(R.id.iconView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)

        fun bind(call: Call) {
            when (call) {
                is Call.Text ->
                    iconView.setImageResource(R.drawable.kenes_ic_chat)
                is Call.Audio ->
                    iconView.setImageResource(R.drawable.kenes_ic_phone)
                is Call.Video ->
                    iconView.setImageResource(R.drawable.kenes_ic_camera_filled)
            }

            titleView.text = call.title

            itemView.setOnClickListener { callback(call) }
        }
    }

}