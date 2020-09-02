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
import q19.kenes_widget.util.inflate
import q19.kenes_widget.util.removeCompoundDrawables

internal class CategoryAdapter(
    private var isExpandable: Boolean,
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
            if (DEFAULT_SIZE_THRESHOLD >= (category?.children?.size ?: 0)) {
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
        if (isExpandable) {
            var itemCount = size

            if (size < 0) {
                itemCount = DEFAULT_SIZE_THRESHOLD
            }

            category?.let {
                if (!it.children.isNullOrEmpty() && size >= it.children.size) {
                    itemCount = it.children.size
                }
            } ?: return 0

            if ((category?.children?.size ?: 0) > DEFAULT_SIZE_THRESHOLD) {
                itemCount += 1
            }

            return itemCount
        } else {
            return if (isSeparateFooterEnabled) {
                getActualSize() + 1
            } else {
                getActualSize()
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
            category?.let { holder.bind(it, position) }
        } else if (holder is FooterViewHolder) {
            holder.bind()
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val imageView = view.findViewById<AppCompatImageView>(R.id.imageView)

        fun bind(category: Category, position: Int) {
            val child = category.children[position]

            textView?.text = child.title

            if (category.responses.isNullOrEmpty()) {
                imageView?.setImageResource(R.drawable.kenes_ic_caret_right_blue)
                imageView?.visibility = View.VISIBLE
            } else {
                imageView?.setImageResource(R.drawable.kenes_ic_document_blue)
                imageView?.visibility = View.VISIBLE
            }

            itemView.isClickable = true
            itemView.isFocusable = true

            itemView.background = buildRippleDrawable(itemView.context)

            itemView.setOnClickListener {
                callback.onCategoryChildClicked(child)
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

    private val paint: Paint = Paint()

    init {
        paint.color = ContextCompat.getColor(context, R.color.kenes_very_light_grayish_blue)
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = (parent.adapter as? CategoryAdapter?) ?: return

        val itemCount = parent.childCount
//        val itemCount = adapter.itemCount

        if (parent.childCount == 1) {
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
            for (index in 0 until itemCount) {
                val child = parent.getChildAt(index)

                val layoutParams = child.layoutParams as RecyclerView.LayoutParams

                val viewType = adapter.getItemViewType(index)

                if (viewType == CategoryAdapter.VIEW_TYPE_HORIZONTAL_BUTTON) {
                    if (itemCount == 1) {
                        val path = getPathOfRoundedRectF(
                            child,
                            radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                        )
                        c.drawPath(path, paint)
                    } else if (itemCount == 2) {
                        if (index == 0) {
                            val path = getPathOfQuadTopRectF(
                                child,
                                radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                            )
                            c.drawPath(path, paint)
                        } else if (index == 1) {
                            val path = getPathOfQuadBottomRectF(
                                child,
                                radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                            )
                            c.drawPath(path, paint)
                        }
                    } else {
                        if (adapter.isSeparateFooterEnabled) {
                            when (index) {
                                0 -> {
                                    val path = getPathOfQuadTopRectF(
                                        child,
                                        radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                    )
                                    c.drawPath(path, paint)
                                }
                                itemCount - 2 -> {
                                    val path = getPathOfQuadBottomRectF(
                                        child,
                                        radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                    )
                                    c.drawPath(path, paint)
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
                        } else {
                            if (index == 0) {
                                val path = getPathOfQuadTopRectF(
                                    child,
                                    radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                )
                                c.drawPath(path, paint)
                            } else {
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
                    if (itemCount > 3 && index < itemCount - 1) {
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
                    when (itemCount) {
                        1 -> RoundMode.ALL
                        2 -> {
                            when (position) {
                                0 -> RoundMode.TOP
                                1 -> RoundMode.BOTTOM
                                else -> RoundMode.NONE
                            }
                        }
                        else -> {
                            if (adapter.isSeparateFooterEnabled) {
                                when (position) {
                                    0 -> RoundMode.TOP
                                    itemCount - 2 -> RoundMode.BOTTOM
                                    else -> RoundMode.NONE
                                }
                            } else {
                                if (position == 0) {
                                    RoundMode.TOP
                                } else {
                                    RoundMode.NONE
                                }
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