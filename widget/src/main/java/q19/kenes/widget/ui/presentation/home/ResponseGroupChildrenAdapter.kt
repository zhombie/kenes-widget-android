package q19.kenes.widget.ui.presentation.home

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.utils.html.HTMLCompat
import kz.q19.utils.view.inflate
import q19.kenes.widget.domain.model.Element
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.domain.model.ResponseInfo
import q19.kenes_widget.R

internal class ResponseGroupChildrenAdapter constructor(
    private val isExpandable: Boolean,
    size: Int = DEFAULT_SIZE_THRESHOLD,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ResponseGroupChildrenAdapter::class.java.simpleName

        private const val DEFAULT_EMPTY_SIZE = 0
        private const val DEFAULT_SIZE_THRESHOLD = 2
    }

    private object Layout {
        val EMPTY_RESPONSE_GROUP = R.layout.cell_empty_response_group
        val RESPONSE_GROUP_CHILD = R.layout.cell_response_group_child
        val RESPONSE_INFO = R.layout.cell_response_info
        val SHOW_ALL_RESPONSE_GROUP_CHILDREN = R.layout.cell_show_all_response_group_children
    }

    private object ViewType {
        const val EMPTY = 99
        const val CHILD = 100
        const val RESPONSE_INFO = 101
        const val SHOW_ALL = 102
    }

    var children: List<Element>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var size: Int = size
        set(value) {
            if (isExpandable) {
                field = value
                notifyDataSetChanged()
            }
        }

    private var isFooterEnabled: Boolean = false

    private fun getItem(position: Int): Element? {
        return if (children.isNullOrEmpty()) {
            null
        } else {
            children?.get(position)
        }
    }

    private fun getActualSize(): Int {
        return children?.size ?: 0
    }

    private fun isCollapsed(): Boolean {
        return if (isExpandable) {
            size == DEFAULT_SIZE_THRESHOLD
        } else {
            false
        }
    }

    private fun toggle(): Int {
        return if (isExpandable) {
            size = if (isCollapsed()) {
                getActualSize()
            } else {
                DEFAULT_SIZE_THRESHOLD
            }
            size
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (itemCount == DEFAULT_EMPTY_SIZE) {
            return when (val item = getItem(position)) {
                is ResponseGroup -> {
                    if (item.children.isEmpty()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.CHILD
                    }
                }
                is ResponseGroup.Child -> {
                    if (item.responses.isEmpty()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.CHILD
                    }
                }
                is ResponseInfo -> {
                    ViewType.RESPONSE_INFO
                }
                else -> ViewType.EMPTY
            }
        }

        fun any(): Int {
            return when (val item = getItem(position)) {
                is ResponseGroup ->
                    ViewType.CHILD
                is ResponseGroup.Child -> {
                    if (item.responses.isEmpty()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.CHILD
                    }
                }
                is ResponseInfo ->
                    ViewType.RESPONSE_INFO
                else ->
                    ViewType.CHILD
            }
        }

        return if (isExpandable) {
            if (DEFAULT_SIZE_THRESHOLD >= getActualSize()) {
                any()
            } else {
                if (position == itemCount - 1) {
                    ViewType.SHOW_ALL
                } else {
                    any()
                }
            }
        } else {
            any()
        }
    }

    override fun getItemCount(): Int {
        isFooterEnabled = false

        val actualSize = getActualSize()

        if (actualSize == 0) {
            return DEFAULT_EMPTY_SIZE
        }

        return if (isExpandable) {
            var itemCount = size

            if (size < 0) {
                itemCount = DEFAULT_SIZE_THRESHOLD
            }

            if (!children.isNullOrEmpty() && size >= actualSize) {
                itemCount = actualSize
            }

            if (actualSize > DEFAULT_SIZE_THRESHOLD) {
                isFooterEnabled = true
                itemCount += 1
            }

            itemCount
        } else {
            actualSize
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EMPTY ->
                EmptyViewHolder(parent.inflate(Layout.EMPTY_RESPONSE_GROUP))
            ViewType.CHILD ->
                ChildViewHolder(parent.inflate(Layout.RESPONSE_GROUP_CHILD))
            ViewType.RESPONSE_INFO ->
                ResponseInfoViewHolder(parent.inflate(Layout.RESPONSE_INFO))
            ViewType.SHOW_ALL ->
                ShowAllViewHolder(parent.inflate(Layout.SHOW_ALL_RESPONSE_GROUP_CHILDREN))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyViewHolder -> holder.bind()
            is ChildViewHolder -> {
                val item = getItem(position)
                if (item != null) {
                    holder.bind(item)
                }
            }
            is ResponseInfoViewHolder -> {
                val item = getItem(position)
                if (item is ResponseInfo) {
                    holder.bind(item)
                }
            }
            is ShowAllViewHolder -> holder.bind()
        }
    }

    private inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
        }
    }

    private inner class ChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<AppCompatImageView>(R.id.iconView)
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val arrowView = view.findViewById<AppCompatImageView>(R.id.arrowView)

        fun bind(element: Element) {
            when (element) {
                is ResponseGroup -> {
                    iconView?.setImageResource(R.drawable.ic_folder)
                    iconView?.visibility = View.VISIBLE

                    textView?.text = element.title

                    arrowView?.setImageResource(R.drawable.ic_arrow_right)
                    arrowView?.visibility = View.VISIBLE

                    itemView.setOnClickListener {
                        callback.onResponseGroupClicked(element)
                    }
                }
                is ResponseGroup.Child -> {
                    iconView?.setImageResource(R.drawable.ic_article)
                    iconView?.visibility = View.VISIBLE

                    textView?.text = element.title

                    arrowView?.setImageDrawable(null)
                    arrowView?.visibility = View.GONE

                    itemView.setOnClickListener {
                        callback.onResponseGroupChildClicked(element)
                    }
                }
                else -> {
                    iconView?.setImageDrawable(null)
                    iconView?.visibility = View.GONE

                    textView?.text = null

                    arrowView?.setImageDrawable(null)
                    arrowView?.visibility = View.GONE

                    itemView.setOnClickListener(null)
                }
            }
        }
    }

    private inner class ResponseInfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(responseInfo: ResponseInfo) {
            if (responseInfo.text.isNullOrBlank()) {
                textView.text = null
            } else {
                textView.text = HTMLCompat.fromHtml(responseInfo.text)
            }
        }
    }

    private inner class ShowAllViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)

        fun bind() {
            if (isCollapsed()) {
                imageView.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_arrow_down
                    )
                )

                textView.setText(R.string.show_all)
            } else {
                imageView.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_arrow_up
                    )
                )

                textView.setText(R.string.collapse_list)
            }

            itemView.setOnClickListener { toggle() }
        }
    }

    interface Callback {
        fun onResponseGroupClicked(responseGroup: ResponseGroup)
        fun onResponseGroupChildClicked(child: ResponseGroup.Child)
        fun onGoBackButtonClicked(responseGroup: ResponseGroup)
    }

}