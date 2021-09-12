package kz.q19.kenes.widget.ui.presentation.home

import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.domain.model.knowledge_base.Element
import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.kenes.widget.R
import kz.q19.utils.html.HTMLCompat
import kz.q19.utils.view.inflate

// TODO: Control expanded/collapsed state in more proper way
internal class ResponseGroupChildrenAdapter constructor(
    var isExpandable: Boolean,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ResponseGroupChildrenAdapter::class.java.simpleName

        const val DEFAULT_EMPTY_SIZE = 0
        const val DEFAULT_SIZE_THRESHOLD = 2
    }

    private object Layout {
        val EMPTY = R.layout.kenes_cell_empty
        val RESPONSE_GROUP_CHILD = R.layout.kenes_cell_response_group_child
        val RESPONSE_GROUP_CHILDREN_FOOTER = R.layout.kenes_cell_response_group_children_footer
        val RESPONSE = R.layout.kenes_cell_response
    }

    private object ViewType {
        const val EMPTY = 99
        const val RESPONSE_GROUP_CHILD = 100
        const val RESPONSE = 101
        const val RESPONSE_GROUP_FOOTER = 102
    }

    var children: List<Element>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var size: Int = DEFAULT_SIZE_THRESHOLD
        set(value) {
            if (isExpandable) {
                field = value
            }
        }

    private fun getItem(position: Int): Element? {
        return if (children.isNullOrEmpty()) {
            null
        } else {
            if (position > (children?.size ?: 0) - 1) {
                null
            } else {
                children?.get(position)
            }
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
            notifyDataSetChanged()
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
                        ViewType.RESPONSE_GROUP_CHILD
                    }
                }
                is ResponseGroup.Child -> {
                    if (item.responses.isEmpty()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.RESPONSE_GROUP_CHILD
                    }
                }
                is Response -> {
                    if (item.text.isNullOrBlank()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.RESPONSE
                    }
                }
                else -> ViewType.EMPTY
            }
        }

        fun any(): Int {
            return when (val item = getItem(position)) {
                is ResponseGroup ->
                    ViewType.RESPONSE_GROUP_CHILD
                is ResponseGroup.Child -> {
                    if (item.responses.isEmpty()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.RESPONSE_GROUP_CHILD
                    }
                }
                is Response ->
                    if (item.text.isNullOrBlank()) {
                        ViewType.EMPTY
                    } else {
                        ViewType.RESPONSE
                    }
                else ->
                    ViewType.RESPONSE_GROUP_CHILD
            }
        }

        return if (isExpandable) {
            if (DEFAULT_SIZE_THRESHOLD >= getActualSize()) {
                any()
            } else {
                if (position == itemCount - 1) {
                    ViewType.RESPONSE_GROUP_FOOTER
                } else {
                    any()
                }
            }
        } else {
            any()
        }
    }

    override fun getItemCount(): Int {
        val actualSize = getActualSize()

        if (actualSize == 0) {
            return DEFAULT_EMPTY_SIZE
        }

        return if (isExpandable) {
            var itemCount = size

            if (itemCount < 0) {
                itemCount = DEFAULT_SIZE_THRESHOLD
            }

            if (itemCount >= actualSize) {
                itemCount = actualSize
            }

            if (actualSize > DEFAULT_SIZE_THRESHOLD) {
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
                EmptyViewHolder(parent.inflate(Layout.EMPTY))
            ViewType.RESPONSE_GROUP_CHILD ->
                ResponseGroupChildViewHolder(parent.inflate(Layout.RESPONSE_GROUP_CHILD))
            ViewType.RESPONSE_GROUP_FOOTER ->
                ResponseGroupFooterViewHolder(parent.inflate(Layout.RESPONSE_GROUP_CHILDREN_FOOTER))
            ViewType.RESPONSE ->
                ResponseViewHolder(parent.inflate(Layout.RESPONSE))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyViewHolder -> holder.bind()
            is ResponseGroupChildViewHolder -> {
                val item = getItem(position)
                if (item != null) {
                    holder.bind(item)
                }
            }
            is ResponseGroupFooterViewHolder -> holder.bind()
            is ResponseViewHolder -> {
                val item = getItem(position)
                if (item is Response) {
                    holder.bind(item)
                }
            }
        }
    }

    private inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
        }
    }

    private inner class ResponseGroupChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<ShapeableImageView>(R.id.iconView)
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)
        private val arrowView = view.findViewById<ShapeableImageView>(R.id.arrowView)

        fun bind(element: Element) {
            when (element) {
                is ResponseGroup -> {
                    iconView?.setImageResource(R.drawable.kenes_ic_folder)
                    iconView?.visibility = View.VISIBLE

                    textView?.text = element.title

                    arrowView?.setImageResource(R.drawable.kenes_ic_arrow_right)
                    arrowView?.visibility = View.VISIBLE

                    itemView.setOnClickListener {
                        callback.onResponseGroupClicked(element)
                    }
                }
                is ResponseGroup.Child -> {
                    iconView?.setImageResource(R.drawable.kenes_ic_article)
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

    private inner class ResponseGroupFooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)
        private val imageView = view.findViewById<ShapeableImageView>(R.id.imageView)

        private val transition = TransitionInflater.from(itemView.context)
            .inflateTransition(R.transition.toggle)

        fun bind() {
            if (isCollapsed()) {
                imageView.setImageDrawable(
                    AppCompatResources.getDrawable(itemView.context, R.drawable.kenes_ic_arrow_down)
                )

                textView.setText(R.string.kenes_show_all)
            } else {
                imageView.setImageDrawable(
                    AppCompatResources.getDrawable(itemView.context, R.drawable.kenes_ic_arrow_up)
                )

                textView.setText(R.string.kenes_collapse_list)
            }

            itemView.setOnClickListener {
                val parent = itemView.parent as? ViewGroup ?: return@setOnClickListener
                TransitionManager.beginDelayedTransition(parent, transition)
                toggle()
            }
        }
    }

    private inner class ResponseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<MaterialTextView>(R.id.textView)

        fun bind(response: Response) {
            val text = response.text
            if (text.isNullOrBlank()) {
                textView.text = null
            } else {
                textView.text = HTMLCompat.fromHtml(text)
            }
        }
    }

    interface Callback {
        fun onResponseGroupClicked(responseGroup: ResponseGroup)
        fun onResponseGroupChildClicked(child: ResponseGroup.Child)
        fun onGoBackButtonClicked(element: Element)
    }

}