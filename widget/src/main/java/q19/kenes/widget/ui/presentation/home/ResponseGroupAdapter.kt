package q19.kenes.widget.ui.presentation.home

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.utils.textview.removeCompoundDrawables
import kz.q19.utils.view.inflate
import q19.kenes.widget.domain.model.AnyResponse
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes_widget.R

internal class ResponseGroupAdapter constructor(
    var isExpandable: Boolean,
    var isSeparateFooterEnabled: Boolean,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TAG = ResponseGroupAdapter::class.java.simpleName

        private const val DEFAULT_EMPTY_SIZE = 1
        private const val DEFAULT_SIZE_THRESHOLD = 2
    }

    private object Layout {
        val RESPONSE_GROUP_CHILD = R.layout.cell_response_group_child
    }

    object ViewType {
        const val EMPTY = 99
        const val HORIZONTAL_BUTTON = 100
        const val FOOTER = 101
    }

    var responseGroup: ResponseGroup? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var size: Int = DEFAULT_SIZE_THRESHOLD
        set(value) {
            if (isExpandable) {
                field = value
                notifyDataSetChanged()
            }
        }

    var isFooterEnabled: Boolean = false
        private set

    private fun getItem(position: Int): AnyResponse? {
        return if (responseGroup?.children.isNullOrEmpty()) {
            null
        } else {
            responseGroup?.children?.get(position)
        }
    }

    private fun getActualSize(): Int {
        return responseGroup?.children?.size ?: 0
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
        if (itemCount == DEFAULT_EMPTY_SIZE) return ViewType.EMPTY
        return if (isExpandable) {
            if (DEFAULT_SIZE_THRESHOLD >= getActualSize()) {
                ViewType.HORIZONTAL_BUTTON
            } else {
                if (position == itemCount - 1) {
                    ViewType.FOOTER
                } else {
                    ViewType.HORIZONTAL_BUTTON
                }
            }
        } else {
            if (isSeparateFooterEnabled) {
                if (position == itemCount - 1) {
                    ViewType.FOOTER
                } else {
                    ViewType.HORIZONTAL_BUTTON
                }
            } else {
                ViewType.HORIZONTAL_BUTTON
            }
        }
    }

    override fun getItemCount(): Int {
        isFooterEnabled = false

        val actualSize = getActualSize()

        if (actualSize == 0) {
            return DEFAULT_EMPTY_SIZE
        }

        if (isExpandable) {
            var itemCount = size

            if (size < 0) {
                itemCount = DEFAULT_SIZE_THRESHOLD
            }

            if (!responseGroup?.children.isNullOrEmpty() && size >= actualSize) {
                itemCount = actualSize
            }

            if (actualSize > DEFAULT_SIZE_THRESHOLD) {
                isFooterEnabled = true
                itemCount += 1
            }

            return itemCount
        } else {
            return if (isSeparateFooterEnabled) {
                isFooterEnabled = true
                actualSize + 1
            } else {
                actualSize
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EMPTY ->
                EmptyViewHolder(parent.inflate(Layout.RESPONSE_GROUP_CHILD))
            ViewType.HORIZONTAL_BUTTON ->
                ViewHolder(parent.inflate(Layout.RESPONSE_GROUP_CHILD))
            ViewType.FOOTER ->
                FooterViewHolder(parent.inflate(Layout.RESPONSE_GROUP_CHILD))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyViewHolder -> holder.bind()
            is ViewHolder -> {
                val item = getItem(position)
                if (item != null) {
                    holder.bind(item)
                }
            }
            is FooterViewHolder -> holder.bind()
        }
    }

    private inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<AppCompatImageView>(R.id.iconView)
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val arrowView = view.findViewById<AppCompatImageView>(R.id.arrowView)

        fun bind() {
            iconView.visibility = View.GONE

            textView.text = "Empty"

            arrowView.visibility = View.GONE
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<AppCompatImageView>(R.id.iconView)
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val arrowView = view.findViewById<AppCompatImageView>(R.id.arrowView)

        fun bind(anyResponse: AnyResponse) {
            when (anyResponse) {
                is ResponseGroup -> {
                    iconView?.setImageResource(R.drawable.ic_folder)
                    iconView?.visibility = View.VISIBLE

                    textView?.text = anyResponse.title

                    arrowView?.setImageResource(R.drawable.ic_arrow_right)
                    arrowView?.visibility = View.VISIBLE

                    itemView.setOnClickListener {
                        callback.onResponseGroupClicked(anyResponse)
                    }
                }
                is ResponseGroup.Child -> {
                    iconView?.setImageResource(R.drawable.ic_article)
                    iconView?.visibility = View.VISIBLE

                    textView?.text = anyResponse.title

                    arrowView?.setImageResource(R.drawable.ic_arrow_right)
                    arrowView?.visibility = View.VISIBLE

                    itemView.setOnClickListener {
                        callback.onResponseGroupChildClicked(anyResponse)
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

    private inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<AppCompatImageView>(R.id.iconView)
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val arrowView = view.findViewById<AppCompatImageView>(R.id.arrowView)

        fun bind() {
            iconView?.setImageDrawable(null)
            iconView?.visibility = View.GONE

            textView?.removeCompoundDrawables()

            if (isExpandable) {
                textView?.setTextColor(ContextCompat.getColor(itemView.context, R.color.kenes_grayish_blue))

                if (isCollapsed()) {
                    textView?.setText(R.string.kenes_show_all)

                    itemView.visibility = View.VISIBLE
                } else {
                    textView?.setText(R.string.kenes_hide)

                    itemView.visibility = View.VISIBLE
                }
            } else {
                if (isSeparateFooterEnabled) {
                    textView?.setTextColor(ContextCompat.getColor(itemView.context, R.color.kenes_bright_blue))
                    textView?.setText(R.string.kenes_back)

                    itemView.visibility = View.VISIBLE
                } else {
                    textView?.text = null

                    itemView.visibility = View.GONE
                }
            }

            arrowView?.setImageDrawable(null)
            arrowView?.visibility = View.GONE

            if (isSeparateFooterEnabled) {
                itemView.setOnClickListener {
                    val responseGroup = responseGroup
                    if (responseGroup != null) {
                        callback.onGoBackButtonClicked(responseGroup)
                    }
                }
            } else {
                itemView.setOnClickListener { toggle() }
            }
        }

    }

    interface Callback {
        fun onResponseGroupClicked(responseGroup: ResponseGroup)
        fun onResponseGroupChildClicked(child: ResponseGroup.Child)
        fun onGoBackButtonClicked(responseGroup: ResponseGroup)
    }

}