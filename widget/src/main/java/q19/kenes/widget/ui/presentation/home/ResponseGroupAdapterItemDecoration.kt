package q19.kenes.widget.ui.presentation.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kz.q19.utils.view.outlineprovider.RoundMode
import kz.q19.utils.view.outlineprovider.RoundOutlineProvider
import q19.kenes.widget.ui.util.getPathOfQuadBottomRectF
import q19.kenes.widget.ui.util.getPathOfQuadTopRectF
import q19.kenes.widget.ui.util.getPathOfRoundedRectF
import q19.kenes.widget.util.Logger
import q19.kenes_widget.R

class ResponseGroupAdapterItemDecoration constructor(
    context: Context,
    strokeWidth: Float,
    private val cornerRadius: Float
) : RecyclerView.ItemDecoration() {

    companion object {
        private val TAG = ResponseGroupAdapterItemDecoration::class.java.simpleName
    }

    private val paint: Paint = Paint()

    init {
        paint.color = ContextCompat.getColor(context, R.color.kenes_very_light_grayish_blue)
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = (parent.adapter as? ResponseGroupAdapter?) ?: return

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

                if (viewType == ResponseGroupAdapter.ViewType.HORIZONTAL_BUTTON) {
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

                } else if (viewType == ResponseGroupAdapter.ViewType.FOOTER) {
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

        val adapter = (parent.adapter as? ResponseGroupAdapter?) ?: return

//        val itemCount = parent.childCount
        val itemCount = adapter.itemCount

        val position = parent.layoutManager?.getPosition(view) ?: -1

        val viewType = adapter.getItemViewType(position)

        if (adapter.isSeparateFooterEnabled) {
            if (viewType == ResponseGroupAdapter.ViewType.FOOTER) {
                outRect.top = parent.context.resources.getDimensionPixelOffset(R.dimen.kenes_footer_vertical_offset)
            } else {
                outRect.setEmpty()
            }
        }

        // Draw rounded background
        if (cornerRadius.compareTo(0f) != 0) {
            val roundMode = when (viewType) {
                ResponseGroupAdapter.ViewType.HORIZONTAL_BUTTON -> {
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
                ResponseGroupAdapter.ViewType.FOOTER -> {
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