package q19.kenes.widget.ui.presentation.home

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import kz.q19.utils.view.inflate
import q19.kenes.widget.domain.model.Element
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes_widget.R

internal class ResponseGroupsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    private var callback: Callback? = null

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

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
        private val toolbar = view.findViewById<FrameLayout>(R.id.toolbar)
        private val backButton = view.findViewById<MaterialButton>(R.id.backButton)
        private val menuButton = view.findViewById<MaterialButton>(R.id.menuButton)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private val headerAdapter = ResponseGroupChildrenHeaderAdapter()
        private val childrenAdapter =
            ResponseGroupChildrenAdapter(isExpandable = true, callback = this)
        private var concatAdapter: ConcatAdapter? = null

        private val layoutManager =
            LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

        init {
            recyclerView.layoutManager = layoutManager

            concatAdapter = ConcatAdapter(headerAdapter, childrenAdapter)
            recyclerView.adapter = concatAdapter
        }

        fun bind(nestable: Nestable) {
//            Logger.debug(TAG, "bind() -> $nestable")

            when (nestable) {
                is ResponseGroup -> {
                    if (nestable.isPrimary) {
                        toolbar.visibility = View.GONE
                    } else {
                        menuButton.visibility = View.GONE
                        toolbar.visibility = View.VISIBLE
                    }

                    headerAdapter.title = nestable.title

                    childrenAdapter.children = nestable.children
                }
                is ResponseGroup.Child -> {
                    menuButton.visibility = View.VISIBLE
                    toolbar.visibility = View.VISIBLE

                    headerAdapter.title = nestable.title

                    childrenAdapter.children = nestable.responses
                }
                else -> {
                    toolbar.visibility = View.GONE

                    headerAdapter.title = null
                }
            }

            backButton.setOnClickListener { callback?.onGoBackButtonClicked(nestable) }
            menuButton.setOnClickListener { callback?.onMenuButtonClicked() }
        }

        override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
            callback?.onResponseGroupClicked(responseGroup)
        }

        override fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
            callback?.onResponseGroupChildClicked(child)
        }

        override fun onGoBackButtonClicked(element: Element) {
            callback?.onGoBackButtonClicked(element)
        }

    }

    interface Callback {
        fun onGoBackButtonClicked(element: Element)
        fun onMenuButtonClicked()

        fun onResponseGroupClicked(responseGroup: ResponseGroup)
        fun onResponseGroupChildClicked(child: ResponseGroup.Child)
    }

}