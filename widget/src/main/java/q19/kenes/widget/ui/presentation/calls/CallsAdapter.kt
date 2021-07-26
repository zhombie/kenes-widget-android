package q19.kenes.widget.ui.presentation.calls

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

internal class CallsAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = CallsAdapter::class.java.simpleName
    }

    var calls: List<Call> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun getItem(position: Int): Call {
        return calls[position]
    }

    override fun getItemCount(): Int = calls.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(R.layout.cell_call))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) holder.bind(getItem(position))
    }

    private inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val audioCallIconView = view.findViewById<ShapeableImageView>(R.id.audioCallIconView)
        private val videoCallIconView = view.findViewById<ShapeableImageView>(R.id.videoCallIconView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)

        fun bind(call: Call) {
            if (call is CallGroup) {
                when {
                    call.hasAudioAndVideoCall() -> {
                        audioCallIconView.visibility = View.VISIBLE
                        videoCallIconView.visibility = View.VISIBLE
                    }
                    call.hasAudioCall() -> {
                        audioCallIconView.visibility = View.VISIBLE
                        videoCallIconView.visibility = View.GONE
                    }
                    call.hasVideoCall() -> {
                        audioCallIconView.visibility = View.GONE
                        videoCallIconView.visibility = View.VISIBLE
                    }
                }
            } else {
                audioCallIconView.visibility = View.GONE
                videoCallIconView.visibility = View.GONE
            }

            titleView.text = call.title

            itemView.setOnClickListener { callback.onCallClicked(call) }
        }

    }

    interface Callback {
        fun onCallClicked(call: Call)
    }

}