package q19.kenes_widget.ui.presentation.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.data.model.Message
import q19.kenes_widget.ui.util.*
import q19.kenes_widget.util.Logger.debug
import q19.kenes_widget.util.inflate

internal class InlineKeyboardAdapter(
    private val callback: (button: Message.ReplyMarkup.Button) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_KEYBOARD_BUTTON = R.layout.kenes_cell_message_keyboard_button
    }

    var replyMarkup: Message.ReplyMarkup? = null
        set(value) {
            field = value
            buttons = field?.getAllButtons() ?: emptyList()
        }

    private var buttons: List<Message.ReplyMarkup.Button> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int): Message.ReplyMarkup.Button {
        return buttons[position]
    }

    override fun getItemCount(): Int = buttons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(LAYOUT_KEYBOARD_BUTTON))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<TextView>(R.id.textView)

        fun bind(button: Message.ReplyMarkup.Button) {
            textView?.text = button.text

            itemView.background = buildRippleDrawable(itemView.context)

            itemView.setOnClickListener { callback(button) }
        }

    }

}


class InlineKeyboardAdapterItemDecoration(
    context: Context,
    strokeWidth: Float,
    private val cornerRadius: Float
) : RecyclerView.ItemDecoration() {

    companion object {
        private const val TAG = "InlineKeyboardAdapterItemDecoration"
    }

    private val paint: Paint = Paint()

    init {
        paint.color = ContextCompat.getColor(context, R.color.kenes_very_light_grayish_blue)
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val itemCount = parent.childCount

        for (index in 0 until itemCount) {
            debug(TAG, "index: $index")

            val child = parent.getChildAt(index)

//            val layoutParams = child.layoutParams as RecyclerView.LayoutParams

            val layoutManager = parent.layoutManager as? GridLayoutManager?

            val columnsCount = layoutManager?.spanCount ?: 0

            debug(TAG, "onDrawOver() -> columnsCount: $columnsCount")

            if (columnsCount == 1) {
                if (index == itemCount - 1) {
                    val path = getPathOfQuadBottomRectF(
                        child,
                        radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                    )
                    c.drawPath(path, paint)
                } else {
                    val path = getPathOfRoundedRectF(
                        child,
                        radius = 0F
                    )
                    c.drawPath(path, paint)
                }
            } else if (columnsCount == 2) {
                if (itemCount >= 2) {
                    debug(TAG, "itemCount % columnsCount > 0: ${itemCount % columnsCount > 0}, $itemCount, $columnsCount")
                    if (itemCount % columnsCount == 0) {
                        when (index) {
                            itemCount - 2 -> {
                                val path = getPathOfRoundedRectF(
                                    child,
                                    bottomLeftRadius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                )
                                c.drawPath(path, paint)
                            }
                            itemCount - 1 -> {
                                val path = getPathOfRoundedRectF(
                                    child,
                                    bottomRightRadius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                                )
                                c.drawPath(path, paint)
                            }
                            else -> {
                                val path = getPathOfRoundedRectF(
                                    child,
                                    radius = 0F
                                )
                                c.drawPath(path, paint)
                            }
                        }
                    } else {
                        if (index == itemCount - 1) {
                            val path = getPathOfQuadBottomRectF(
                                child,
                                radius = parent.context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
                            )
                            c.drawPath(path, paint)
                        } else {
                            val path = getPathOfRoundedRectF(
                                child,
                                radius = 0F
                            )
                            c.drawPath(path, paint)
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

        val adapter = (parent.adapter as? InlineKeyboardAdapter?) ?: return

        val itemCount = adapter.itemCount
//        val itemCount = parent.childCount

        val position = parent.layoutManager?.getPosition(view) ?: -1

        val layoutManager = parent.layoutManager as? GridLayoutManager?

        val columnsCount = layoutManager?.spanCount ?: 0

        debug(TAG, "getItemOffsets() -> columnsCount: $columnsCount, itemCount: $itemCount, position: $position")

        // Draw rounded background
        if (cornerRadius.compareTo(0f) != 0) {
            val roundMode = if (columnsCount == 1) {
                if (position == itemCount - 1) {
                    RoundMode.BOTTOM
                } else {
                    RoundMode.NONE
                }
            } else if (columnsCount == 2) {
                if (itemCount >= 2) {
                    if (itemCount % columnsCount == 0) {
                        when (position) {
                            itemCount - 2 -> RoundMode.BOTTOM_LEFT
                            itemCount - 1 -> RoundMode.BOTTOM_RIGHT
                            else -> RoundMode.NONE
                        }
                    } else {
                        if (position == itemCount - 1) {
                            RoundMode.BOTTOM
                        } else {
                            RoundMode.NONE
                        }
                    }
                } else {
                    RoundMode.NONE
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