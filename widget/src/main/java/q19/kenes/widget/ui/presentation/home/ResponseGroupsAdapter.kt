package q19.kenes.widget.ui.presentation.home

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.utils.view.inflate
import q19.kenes_widget.R

internal class ResponseGroupsAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ResponseGroupsAdapter::class.java.simpleName
    }

    private object Layout {
        val RESPONSE_GROUP: Int = R.layout.cell_response_group
    }

    private val diffCallback = object : DiffUtil.ItemCallback<ResponseGroup>() {
        override fun areContentsTheSame(oldItem: ResponseGroup, newItem: ResponseGroup): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areItemsTheSame(oldItem: ResponseGroup, newItem: ResponseGroup): Boolean {
            return oldItem.id == newItem.id
        }
    }

    private val asyncListDiffer = AsyncListDiffer(this, diffCallback)

    fun submitList(responseGroups: List<ResponseGroup>) {
        asyncListDiffer.submitList(responseGroups)
    }

    private fun getItem(position: Int): ResponseGroup {
        return asyncListDiffer.currentList[position]
    }

    override fun getItemCount(): Int = asyncListDiffer.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(Layout.RESPONSE_GROUP))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), ResponseGroupAdapter.Callback {
        private val titleView = view.findViewById<TextView>(R.id.titleView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private val adapter: ResponseGroupAdapter
        private val layoutManager: LinearLayoutManager

        init {
            adapter = ResponseGroupAdapter(isExpandable = true, isSeparateFooterEnabled = false, callback = this)
            recyclerView.adapter = adapter

            layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            recyclerView.layoutManager = layoutManager

            recyclerView.addItemDecoration(
                ResponseGroupAdapterItemDecoration(
                    itemView.context,
                    itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_width),
                    itemView.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                )
            )
        }

        fun bind(responseGroup: ResponseGroup) {
            titleView.text = responseGroup.title

            if (adapter.responseGroup == null) {
                adapter.responseGroup = responseGroup
            }
        }

        override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
            callback.onResponseGroupClicked(responseGroup)
        }

        override fun onResponseClicked(response: Response) {
            callback.onResponseClicked(response)
        }

        override fun onGoBackButtonClicked(responseGroup: ResponseGroup) {
            callback.onGoBackButtonClicked(responseGroup)
        }

    }

    interface Callback {
        fun onResponseGroupClicked(responseGroup: ResponseGroup)
        fun onResponseClicked(response: Response)
        fun onGoBackButtonClicked(responseGroup: ResponseGroup)
    }

}