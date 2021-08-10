package q19.kenes.widget.ui.presentation.call

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

internal class CallsAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = CallsAdapter::class.java.simpleName
    }

    private object ViewType {
        const val CALL_GROUP = 100
        const val CALL = 101
    }

    var anyCalls: List<AnyCall> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun getItem(position: Int): AnyCall {
        return anyCalls[position]
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Call -> ViewType.CALL
            is CallGroup -> ViewType.CALL_GROUP
            else -> super.getItemViewType(position)
        }
    }

    override fun getItemCount(): Int = anyCalls.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CALL ->
                CallViewHolder(parent.inflate(R.layout.cell_call))
            ViewType.CALL_GROUP ->
                CallGroupViewHolder(parent.inflate(R.layout.cell_call_group))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is CallGroupViewHolder) {
            if (item is CallGroup) {
                holder.bind(item)
            }
        } else if (holder is CallViewHolder) {
            if (item is Call) {
                holder.bind(item)
            }
        }
    }

    private inner class CallViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<ShapeableImageView>(R.id.iconView)
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)

        fun bind(call: Call) {
            when (call) {
                is Call.Text ->
                    iconView.setImageResource(R.drawable.ic_chat)
                is Call.Audio ->
                    iconView.setImageResource(R.drawable.ic_headphones)
                is Call.Video ->
                    iconView.setImageResource(R.drawable.ic_video_camera)
            }

            titleView.text = call.title

            itemView.setOnClickListener { callback.onCallClicked(call) }
        }
    }

    private inner class CallGroupViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<MaterialTextView>(R.id.titleView)
        private val callTypesHintView = view.findViewById<MaterialTextView>(R.id.callTypesHintView)
        private val callTypesView = view.findViewById<RecyclerView>(R.id.callTypesView)

        private val adapter = CallTypesAdapter()

        init {
            callTypesView.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            callTypesView.adapter = adapter
        }

        fun bind(callGroup: CallGroup) {
            val calls = callGroup.children.filterIsInstance<Call>()
            if (calls.isEmpty()) {
                adapter.calls = emptyList()
                callTypesView.visibility = View.GONE

                callTypesHintView.text = null
                callTypesHintView.visibility = View.GONE
            } else {
                adapter.calls = calls
                callTypesView.visibility = View.VISIBLE

                callTypesHintView.text = "Доступные виды звонков"
                callTypesHintView.visibility = View.GONE
            }

            titleView.text = callGroup.title

            itemView.setOnClickListener { callback.onCallGroupClicked(callGroup) }
        }

    }

    interface Callback {
        fun onCallClicked(call: Call)
        fun onCallGroupClicked(callGroup: CallGroup)
    }

}