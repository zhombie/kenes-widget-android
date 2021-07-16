package q19.kenes.widget.ui.components.deprecated

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.q19.common.error.ViewHolderViewTypeException
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import kz.q19.utils.textview.removeCompoundDrawables
import kz.q19.utils.view.inflate
import kz.q19.utils.view.outlineprovider.RoundMode
import kz.q19.utils.view.outlineprovider.RoundOutlineProvider
import q19.kenes.widget.core.logging.Logger.debug
import q19.kenes.widget.ui.components.base.TitleView
import q19.kenes.widget.ui.util.*
import q19.kenes_widget.R

internal class ServicesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val TAG = "ServicesView"
    }

    private var titleView: TitleView? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: ServicesAdapter? = null

    var callback: Callback? = null

    init {
        orientation = VERTICAL
    }

    private fun ViewGroup?.isViewAdded(view: View?): Boolean {
        if (this == null) return false
        return view != null && this.indexOfChild(view) != -1
    }

    fun showServices(
        parentService: Configs.Service?,
        services: List<Configs.Service>,
        language: Language
    ) {
        if (titleView == null) {
            titleView = TitleView(context)
            titleView?.layoutParams = MarginLayoutParams(
                MarginLayoutParams.MATCH_PARENT,
                MarginLayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(
                    0,
                    0,
                    0,
                    context.resources.getDimensionPixelOffset(R.dimen.kenes_title_bottom_offset)
                )
            }
            titleView?.setPadding(
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                resources.getDimensionPixelOffset(R.dimen.kenes_title_padding),
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                resources.getDimensionPixelOffset(R.dimen.kenes_title_padding)
            )
        }

        if (parentService == null) {
            titleView?.hideBackButtonOnLeft()

            titleView?.setText(R.string.kenes_services)

            titleView?.isClickable = false

            titleView?.removeBackground()

            titleView?.removeClickListeners()

            adapter?.isFooterEnabled = false
        } else {
            titleView?.showBackButtonOnLeft()

            titleView?.text = parentService.title.get(language)

            titleView?.isClickable = true

            titleView?.setRippleBackground()

            if (titleView?.hasOnClickListeners() == false) {
                titleView?.setOnClickListener { callback?.onServiceBackClicked() }
            }

            adapter?.isFooterEnabled = true
        }

        if (!isViewAdded(titleView)) {
            addView(titleView)
        }

        if (recyclerView == null) {
            recyclerView = RecyclerView(context)
            val layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )

            layoutParams.setMargins(
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                0,
                resources.getDimensionPixelOffset(R.dimen.kenes_horizontal_spacing),
                0
            )

            recyclerView?.layoutParams = layoutParams

            recyclerView?.setPadding(
                0,
                0,
                0,
                context.resources.getDimensionPixelOffset(R.dimen.kenes_section_padding)
            )

            recyclerView?.clipToPadding = false

            recyclerView?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        if (!isViewAdded(recyclerView)) {
            addView(recyclerView)
        }

        if (adapter == null) {
            adapter = ServicesAdapter(language, object : ServicesAdapter.Callback {
                override fun onServiceClicked(service: Configs.Service) {
                    callback?.onServiceClicked(service)
                }

                override fun onServiceBackClicked() {
                    callback?.onServiceBackClicked()
                }
            })

            recyclerView?.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            recyclerView?.adapter = adapter

            recyclerView?.addItemDecoration(
                ServicesAdapterItemDecoration(
                context,
                resources.getDimension(R.dimen.kenes_rounded_border_width),
                resources.getDimension(R.dimen.kenes_rounded_border_radius)
            )
            )
        }

        adapter?.services = services
    }

    interface Callback {
        fun onServiceClicked(service: Configs.Service)
        fun onServiceBackClicked()
    }

}


private class ServicesAdapter(
    private val language: Language,
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "ServicesAdapter"

        private val LAYOUT_HORIZONTAL_BUTTON = R.layout.kenes_cell_horizontal_button

        const val VIEW_TYPE_SERVICE = 100
        const val VIEW_TYPE_FOOTER = 101
    }

    var services: List<Configs.Service> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isFooterEnabled: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        return if (isFooterEnabled) {
            if (position == itemCount - 1) {
                VIEW_TYPE_FOOTER
            } else {
                VIEW_TYPE_SERVICE
            }
        } else {
            VIEW_TYPE_SERVICE
        }
    }

    override fun getItemCount(): Int = services.size + if (isFooterEnabled) 1 else 0

    fun getItem(position: Int): Configs.Service? {
        if (position < 0) {
            return null
        }
        return services[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SERVICE -> ServiceViewHolder(parent.inflate(LAYOUT_HORIZONTAL_BUTTON))
            VIEW_TYPE_FOOTER -> FooterViewHolder(parent.inflate(LAYOUT_HORIZONTAL_BUTTON))
            else -> throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ServiceViewHolder) {
            val item = getItem(position)
            item?.let { holder.bind(it) }
        } else if (holder is FooterViewHolder) {
            holder.bind()
        }
    }

    private inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<AppCompatTextView>(R.id.textView)
        private val imageView = view.findViewById<AppCompatImageView>(R.id.imageView)

        fun bind(service: Configs.Service) {
            textView.text = service.title.get(language)

            debug(TAG, "service: $service")

            when (service.type) {
                Configs.Nestable.Type.FOLDER -> {
                    imageView.setImageResource(R.drawable.kenes_ic_caret_right_blue)
                    imageView.visibility = View.VISIBLE

                    textView.removeCompoundDrawables()

                    itemView.isClickable = true
                    itemView.isFocusable = true

                    itemView.background = buildRippleDrawable(itemView.context)

                    itemView.setOnClickListener { callback.onServiceClicked(service) }
                }
                Configs.Nestable.Type.LINK -> {
                    imageView.visibility = View.GONE

                    textView.removeCompoundDrawables()

                    itemView.isClickable = true
                    itemView.isFocusable = true

                    itemView.background = buildRippleDrawable(itemView.context)

                    itemView.setOnClickListener { callback.onServiceClicked(service) }
                }
                else -> {
                    imageView.visibility = View.GONE

                    textView.removeCompoundDrawables()

                    itemView.isClickable = false
                    itemView.isFocusable = false

                    itemView.background = buildSimpleDrawable(itemView.context)

                    itemView.setOnClickListener(null)
                }
            }
        }
    }

    private inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView = view.findViewById<AppCompatTextView>(R.id.textView)

        fun bind() {
            textView.removeCompoundDrawables()

            textView.setText(R.string.kenes_back)

            itemView.isClickable = true
            itemView.isFocusable = true

            itemView.background = buildRippleDrawable(itemView.context)

            itemView.setOnClickListener { callback.onServiceBackClicked() }
        }
    }

    interface Callback {
        fun onServiceClicked(service: Configs.Service)
        fun onServiceBackClicked()
    }

}


private class ServicesAdapterItemDecoration(
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
        val adapter = (parent.adapter as? ServicesAdapter?) ?: return

        val itemCount = parent.childCount
//
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

                if (viewType == ServicesAdapter.VIEW_TYPE_FOOTER) {
                    val path = getPathOfRoundedRectF(
                        child,
                        radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                    )
                    c.drawPath(path, paint)
                } else if (viewType == ServicesAdapter.VIEW_TYPE_SERVICE) {
                    val relationalItemCount = if (adapter.isFooterEnabled) {
                        itemCount - 1
                    } else {
                        itemCount
                    }

                    // Divider
                    if (itemCount > 3 && index < relationalItemCount - 1) {
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

                    if (relationalItemCount == 1) {
                        val path = getPathOfRoundedRectF(
                            child,
                            radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                        )
                        c.drawPath(path, paint)
                    } else {
                        when (index) {
                            0 -> {
                                val path = getPathOfQuadTopRectF(
                                    child,
                                    radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                )
                                c.drawPath(path, paint)
                            }
                            relationalItemCount - 1 -> {
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

        val adapter = (parent.adapter as? ServicesAdapter?) ?: return

        val itemCount = adapter.itemCount
//        val itemCount = parent.childCount

        val position = parent.layoutManager?.getPosition(view) ?: -1

        val viewType = adapter.getItemViewType(position)

        // Offset between elements
        when (viewType) {
            ServicesAdapter.VIEW_TYPE_FOOTER -> {
                outRect.top = parent.context.resources.getDimensionPixelOffset(R.dimen.kenes_footer_vertical_offset)
            }
            ServicesAdapter.VIEW_TYPE_SERVICE -> {
                outRect.setEmpty()
            }
            else -> {
                outRect.setEmpty()
            }
        }

        // Draw rounded background
        if (cornerRadius.compareTo(0f) != 0) {
            val roundMode = if (viewType == ServicesAdapter.VIEW_TYPE_FOOTER) {
                RoundMode.ALL
            } else if (viewType == ServicesAdapter.VIEW_TYPE_SERVICE) {
                val relationalItemCount = if (adapter.isFooterEnabled) {
                    itemCount - 1
                } else {
                    itemCount
                }

                if (relationalItemCount == 1) {
                    RoundMode.ALL
                } else {
                    when (parent.getChildAdapterPosition(view)) {
                        0 -> RoundMode.TOP
                        relationalItemCount - 1 -> RoundMode.BOTTOM
                        else -> RoundMode.NONE
                    }
                }
            } else {
                RoundMode.NONE
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