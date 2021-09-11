package q19.kenes.widget.ui.presentation.call

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kz.q19.domain.model.call.Call
import kz.q19.utils.view.binding.bind
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

internal class CallTypesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = CallTypesAdapter::class.java.simpleName
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
        return ViewHolder(parent.inflate(R.layout.kenes_cell_call_type))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView by bind<ShapeableImageView>(R.id.iconView)

        fun bind(call: Call) {
            when (call) {
                is Call.Text ->
                    iconView.setImageResource(R.drawable.kenes_ic_chat)
                is Call.Audio ->
                    iconView.setImageResource(R.drawable.kenes_ic_phone)
                is Call.Video ->
                    iconView.setImageResource(R.drawable.kenes_ic_camera_filled)
            }
        }
    }

}