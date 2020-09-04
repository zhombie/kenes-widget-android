package q19.kenes_widget.ui.presentation.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.core.errors.ViewHolderViewTypeException
import q19.kenes_widget.data.model.Category
import q19.kenes_widget.ui.util.*
import q19.kenes_widget.util.Logger
import q19.kenes_widget.util.inflate
import q19.kenes_widget.util.isVisible
import q19.kenes_widget.util.removeCompoundDrawables

internal class CategoryAdapter(
    var isExpandable: Boolean,
    var isSeparateFooterEnabled: Boolean,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
//        private const val TAG = "CategoryAdapter"

        private val LAYOUT_HORIZONTAL_BUTTON = R.layout.kenes_cell_horizontal_button
        private val LAYOUT_FOOTER = R.layout.kenes_cell_horizontal_button

        private const val DEFAULT_SIZE_THRESHOLD = 2

        const val VIEW_TYPE_HORIZONTAL_BUTTON = 100
        const val VIEW_TYPE_FOOTER = 101
    }

    var category: Category? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var size: Int = DEFAULT_SIZE_THRESHOLD
        set(value) {
            if (isExpandable) {
                field = value
                notifyDataSetChanged()
            } else {
                return
            }
        }

    var isFooterEnabled: Boolean = false
        private set

    private fun getItem(position: Int): Category? {
        return category?.children?.get(position)
    }

    private fun getActualSize(): Int {
        return category?.children?.size ?: 0
    }

    private fun isCollapsed(): Boolean {
        return if (isExpandable) {
            size == DEFAULT_SIZE_THRESHOLD
        } else {
            false
        }
    }

    fun toggle(): Int {
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
//        Logger.debug(TAG, "getItemViewType() -> position: $position")
        return if (isExpandable) {
            if (DEFAULT_SIZE_THRESHOLD >= getActualSize()) {
                VIEW_TYPE_HORIZONTAL_BUTTON
            } else {
                if (position == itemCount - 1) {
                    VIEW_TYPE_FOOTER
                } else {
                    VIEW_TYPE_HORIZONTAL_BUTTON
                }
            }
        } else {
            if (isSeparateFooterEnabled) {
                if (position == itemCount - 1) {
                    VIEW_TYPE_FOOTER
                } else {
                    VIEW_TYPE_HORIZONTAL_BUTTON
                }
            } else {
                VIEW_TYPE_HORIZONTAL_BUTTON
            }
        }
    }

    override fun getItemCount(): Int {
        isFooterEnabled = false

        val actualSize = getActualSize()

        if (isExpandable) {
            var itemCount = size

            if (size < 0) {
                itemCount = DEFAULT_SIZE_THRESHOLD
            }

            if (!category?.children.isNullOrEmpty() && size >= actualSize) {
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
            VIEW_TYPE_HORIZONTAL_BUTTON ->
                ViewHolder(parent.inflate(LAYOUT_HORIZONTAL_BUTTON))
            VIEW_TYPE_FOOTER ->
                FooterViewHolder(parent.inflate(LAYOUT_FOOTER))
            else ->
                throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        } else if (holder is FooterViewHolder) {
            holder.bind()
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val imageView = view.findViewById<AppCompatImageView>(R.id.imageView)

        fun bind(category: Category?) {
//            Logger.debug(TAG, "category: category")

            textView?.text = category?.title

            if (category == Category.EMPTY) {
                imageView?.setImageDrawable(null)
                imageView?.isVisible = false

                itemView.isClickable = false
                itemView.isFocusable = false

                itemView.background = buildSimpleDrawable(itemView.context)
            } else {
                if (category?.responses.isNullOrEmpty()) {
                    imageView?.setImageResource(R.drawable.kenes_ic_caret_right_blue)
                    imageView?.isVisible = true
                } else {
                    imageView?.setImageResource(R.drawable.kenes_ic_document_blue)
                    imageView?.isVisible = true
                }

                itemView.isClickable = true
                itemView.isFocusable = true

                itemView.background = buildRippleDrawable(itemView.context)

                itemView.setOnClickListener {
                    if (category != null) {
                        callback.onCategoryChildClicked(category)
                    }
                }
            }
        }
    }

    private inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val imageView = view.findViewById<AppCompatImageView>(R.id.imageView)

        fun bind() {
            imageView?.visibility = View.GONE

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

            itemView.isClickable = true
            itemView.isFocusable = true

            itemView.background = buildRippleDrawable(itemView.context)

            if (isSeparateFooterEnabled) {
                itemView.setOnClickListener {
                    category?.let {
                        callback.onGoBackButtonClicked(it)
                    }
                }
            } else {
                itemView.setOnClickListener { toggle() }
            }
        }

    }

    interface Callback {
        fun onCategoryChildClicked(category: Category)
        fun onGoBackButtonClicked(category: Category)
    }

}



internal class CategoryAdapterItemDecoration(
    context: Context,
    strokeWidth: Float,
    private val cornerRadius: Float
) : RecyclerView.ItemDecoration() {

    companion object {
        private const val TAG = "CategoryAdapterItemDecoration"
    }

    private val paint: Paint = Paint()

    init {
        paint.color = ContextCompat.getColor(context, R.color.kenes_very_light_grayish_blue)
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = (parent.adapter as? CategoryAdapter?) ?: return

//        val itemCount = parent.childCount
        val itemCount = adapter.itemCount

        if (itemCount == 1) {
            val child = parent.getChildAt(0)
            c.drawRoundRect(
                child.left.toFloat(),
                child.top.toFloat(),
                child.right.toFloat(),
                child.bottom.toFloat(),
                parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius),
                parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius),
                paint
            )
        } else {
            for (position in 0 until itemCount) {
                val child = parent.getChildAt(position)

                val layoutParams = child.layoutParams as RecyclerView.LayoutParams

                val viewType = adapter.getItemViewType(position)

                if (viewType == CategoryAdapter.VIEW_TYPE_HORIZONTAL_BUTTON) {
                    val relativeItemCount = if (adapter.isFooterEnabled) {
                        itemCount - 1
                    } else {
                        itemCount
                    }

                    if (relativeItemCount == 1) {
                        val path = getPathOfRoundedRectF(
                            child,
                            radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                        )
                        c.drawPath(path, paint)
                    } else {
                        when (position) {
                            0 -> {
                                val path = getPathOfQuadTopRectF(
                                    child,
                                    radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                )
                                c.drawPath(path, paint)
                            }
                            relativeItemCount - 1 -> {
                                if (adapter.isFooterEnabled) {
                                    if (adapter.isExpandable) {
                                        val path = Path()
                                        path.moveTo(child.left.toFloat(), child.top.toFloat())
                                        path.lineTo(child.left.toFloat(), child.bottom.toFloat())
                                        path.moveTo(child.right.toFloat(), child.top.toFloat())
                                        path.lineTo(child.right.toFloat(), child.bottom.toFloat())
                                        path.close()
                                        c.drawPath(path, paint)
                                    } else {
                                        if (adapter.isSeparateFooterEnabled) {
                                            val path = getPathOfQuadBottomRectF(
                                                child,
                                                radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                            )
                                            c.drawPath(path, paint)
                                        }
                                    }
                                } else {
                                    if (adapter.isExpandable) {
                                        val path = getPathOfQuadBottomRectF(
                                            child,
                                            radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                        )
                                        c.drawPath(path, paint)
                                    }
                                }
                            }
                            else -> {
                                val path = Path()
                                path.moveTo(child.left.toFloat(), child.top.toFloat())
                                path.lineTo(child.left.toFloat(), child.bottom.toFloat())
                                path.moveTo(child.right.toFloat(), child.top.toFloat())
                                path.lineTo(child.right.toFloat(), child.bottom.toFloat())
                                path.close()
                                c.drawPath(path, paint)
                            }
                        }
                    }

                    // Divider
                    if (relativeItemCount > 3 && position < relativeItemCount - 1) {
                        val startX = parent.paddingStart
                        val stopX = parent.width
                        val y = child.bottom + layoutParams.bottomMargin
                        c.drawLine(
                            startX.toFloat(),
                            y.toFloat(),
                            stopX.toFloat(),
                            y.toFloat(),
                            paint
                        )
                    }

                } else if (viewType == CategoryAdapter.VIEW_TYPE_FOOTER) {
                    if (adapter.isSeparateFooterEnabled) {
                        val path = getPathOfRoundedRectF(
                            child,
                            radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                        )
                        c.drawPath(path, paint)
                    } else {
                        val path = getPathOfQuadBottomRectF(
                            child,
                            radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                        )
                        c.drawPath(path, paint)
                    }
                }
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val adapter = (parent.adapter as? CategoryAdapter?) ?: return

//        val itemCount = parent.childCount
        val itemCount = adapter.itemCount

        val position = parent.layoutManager?.getPosition(view) ?: -1

        val viewType = adapter.getItemViewType(position)

        if (adapter.isSeparateFooterEnabled) {
            if (viewType == CategoryAdapter.VIEW_TYPE_FOOTER) {
                outRect.top = parent.context.resources.getDimensionPixelOffset(R.dimen.kenes_footer_vertical_offset)
            } else {
                outRect.setEmpty()
            }
        }

        // Draw rounded background
        if (cornerRadius.compareTo(0f) != 0) {
            val roundMode = when (viewType) {
                CategoryAdapter.VIEW_TYPE_HORIZONTAL_BUTTON -> {
                    val relativeItemCount = if (adapter.isFooterEnabled) {
                        itemCount - 1
                    } else {
                        itemCount
                    }

                    Logger.debug(TAG, "relativeItemCount: $relativeItemCount")

                    when (relativeItemCount) {
                        1 -> RoundMode.ALL
                        else -> {
                            when (position) {
                                0 -> RoundMode.TOP
                                relativeItemCount - 1 -> {
                                    if (adapter.isFooterEnabled) {
                                        if (adapter.isExpandable) {
                                            RoundMode.NONE
                                        } else {
                                            if (adapter.isSeparateFooterEnabled) {
                                                RoundMode.BOTTOM
                                            } else {
                                                RoundMode.NONE
                                            }
                                        }
                                    } else {
                                        if (adapter.isExpandable) {
                                            RoundMode.BOTTOM
                                        } else {
                                            RoundMode.NONE
                                        }
                                    }
                                }
                                else -> RoundMode.NONE
                            }
                        }
                    }
                }
                CategoryAdapter.VIEW_TYPE_FOOTER -> {
                    if (adapter.isSeparateFooterEnabled) {
                        RoundMode.ALL
                    } else {
                        RoundMode.BOTTOM
                    }
                }
                else -> RoundMode.NONE
            }

            val outlineProvider = view.outlineProvider
            if (outlineProvider is RoundOutlineProvider) {
                outlineProvider.roundMode = roundMode
                view.invalidateOutline()
            } else {
                view.outlineProvider = RoundOutlineProvider(cornerRadius, roundMode)
                view.clipToOutline = true
            }
        }
    }

}