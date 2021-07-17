package q19.kenes.widget.ui.presentation.home

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.utils.view.inflate
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
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

    private val diffCallback = object : DiffUtil.ItemCallback<Nestable>() {
        override fun areContentsTheSame(oldItem: Nestable, newItem: Nestable): Boolean {
            return if (oldItem is ResponseGroup && newItem is ResponseGroup) {
                oldItem.id == newItem.id && oldItem.title == newItem.title
            } else if (oldItem is ResponseGroup.Child && newItem is ResponseGroup.Child) {
                oldItem.id == newItem.id && oldItem.title == newItem.title
            } else {
                false
            }
        }

        override fun areItemsTheSame(oldItem: Nestable, newItem: Nestable): Boolean {
            return if (oldItem is ResponseGroup && newItem is ResponseGroup) {
                oldItem.id == newItem.id
            } else if (oldItem is ResponseGroup.Child && newItem is ResponseGroup.Child) {
                oldItem.id == newItem.id
            } else {
                false
            }
        }
    }

    private val asyncListDiffer by lazy { AsyncListDiffer(this, diffCallback) }

    fun submitList(nestables: List<Nestable>) {
        asyncListDiffer.submitList(nestables)
    }

    private fun getItem(position: Int): Nestable {
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

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        ResponseGroupChildrenAdapter.Callback {
        private val titleView = view.findViewById<AppCompatTextView>(R.id.titleView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private val adapter = ResponseGroupChildrenAdapter(isExpandable = true, callback = this)

        private val layoutManager =
            LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

        init {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = layoutManager
        }

        fun bind(nestable: Nestable) {
            Logger.debug(TAG, "bind() -> $nestable")

            when (nestable) {
                is ResponseGroup -> {
                    titleView.text = nestable.title

                    adapter.children = nestable.children
                }
                is ResponseGroup.Child -> {
                    titleView.text = nestable.title

                    adapter.children = nestable.responses
                }
                else -> {
                    titleView.text = null
                }
            }
        }

        override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
            callback.onResponseGroupClicked(responseGroup)
        }

        override fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
            callback.onResponseGroupChildClicked(child)
        }

        override fun onGoBackButtonClicked(responseGroup: ResponseGroup) {
            callback.onGoBackButtonClicked(responseGroup)
        }

    }

    interface Callback {
        fun onResponseGroupClicked(responseGroup: ResponseGroup)
        fun onResponseGroupChildClicked(child: ResponseGroup.Child)
        fun onGoBackButtonClicked(responseGroup: ResponseGroup)
    }

}