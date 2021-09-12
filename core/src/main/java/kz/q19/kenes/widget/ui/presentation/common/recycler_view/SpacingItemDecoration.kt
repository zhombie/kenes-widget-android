package kz.q19.kenes.widget.ui.presentation.common.recycler_view

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * Modifies item spacing in a recycler view so that items are equally spaced no matter where they
 * are on the grid. Only designed to work with standard linear or grid layout managers.
 */
internal class SpacingItemDecoration constructor(
    @Px private val pxBetweenItems: Float = 0F
) : RecyclerView.ItemDecoration() {

    companion object {
        private fun shouldReverseLayout(
            layout: RecyclerView.LayoutManager,
            horizontallyScrolling: Boolean
        ): Boolean {
            var reverseLayout = layout is LinearLayoutManager && layout.reverseLayout
            val rtl = layout.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
            if (horizontallyScrolling && rtl) {
                // This is how linearlayout checks if it should reverse layout in #resolveShouldLayoutReverse
                reverseLayout = !reverseLayout
            }
            return reverseLayout
        }

        private fun isInFirstRow(
            position: Int,
            spanSizeLookup: GridLayoutManager.SpanSizeLookup,
            spanCount: Int
        ): Boolean {
            var totalSpan = 0
            for (i in 0..position) {
                totalSpan += spanSizeLookup.getSpanSize(i)
                if (totalSpan > spanCount) {
                    return false
                }
            }
            return true
        }

        private fun isInLastRow(
            position: Int, itemCount: Int, spanSizeLookup: GridLayoutManager.SpanSizeLookup,
            spanCount: Int
        ): Boolean {
            var totalSpan = 0
            for (i in itemCount - 1 downTo position) {
                totalSpan += spanSizeLookup.getSpanSize(i)
                if (totalSpan > spanCount) {
                    return false
                }
            }
            return true
        }
    }

    private var canScrollVertically = false
    private var canScrollHorizontally = false
    private var firstItem = false
    private var lastItem = false
    private var isGridLayout = false

    private var isFirstItemInRow = false
    private var fillsLastSpan = false
    private var isInFirstRow = false
    private var isInLastRow = false

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Zero everything out for the common case
        outRect.setEmpty()
        val position: Int = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) {
            // View is not shown
            return
        }
        val layoutManager: RecyclerView.LayoutManager = parent.layoutManager ?: return
        calculatePositionDetails(parent, position, layoutManager)
        var left = useLeftPadding()
        var right = useRightPadding()
        var top = useTopPadding()
        var bottom = useBottomPadding()
        if (shouldReverseLayout(layoutManager, canScrollHorizontally)) {
            if (canScrollHorizontally) {
                val temp = left
                left = right
                right = temp
            } else {
                val temp = top
                top = bottom
                bottom = temp
            }
        }

        // Divided by two because it is applied to the left side of one item and the right of another
        // to add up to the total desired space
        val padding = (pxBetweenItems / 2F).roundToInt()
        outRect.right = if (right) padding else 0
        outRect.left = if (left) padding else 0
        outRect.top = if (top) padding else 0
        outRect.bottom = if (bottom) padding else 0
    }

    private fun calculatePositionDetails(
        parent: RecyclerView,
        position: Int,
        layout: RecyclerView.LayoutManager
    ) {
        val itemCount = parent.adapter?.itemCount ?: 0
        firstItem = position == 0
        lastItem = position == itemCount - 1
        canScrollHorizontally = layout.canScrollHorizontally()
        canScrollVertically = layout.canScrollVertically()
        if (layout is GridLayoutManager) {
            isGridLayout = true
            val spanSizeLookup: GridLayoutManager.SpanSizeLookup = layout.spanSizeLookup
            val spanSize: Int = spanSizeLookup.getSpanSize(position)
            val spanCount: Int = layout.spanCount
            val spanIndex: Int = spanSizeLookup.getSpanIndex(position, spanCount)
            isFirstItemInRow = spanIndex == 0
            fillsLastSpan = spanIndex + spanSize == spanCount
            isInFirstRow = isInFirstRow(position, spanSizeLookup, spanCount)
            isInLastRow =
                !isInFirstRow && isInLastRow(position, itemCount, spanSizeLookup, spanCount)
        }
    }

    private fun useLeftPadding(): Boolean {
        return if (isGridLayout) {
            canScrollHorizontally && !isInFirstRow || canScrollVertically && !isFirstItemInRow
        } else {
            canScrollHorizontally && !firstItem
        }
    }

    private fun useTopPadding(): Boolean {
        return if (isGridLayout) {
            canScrollHorizontally && !isFirstItemInRow || canScrollVertically && !isInFirstRow
        } else {
            canScrollVertically && !firstItem
        }
    }

    private fun useRightPadding(): Boolean {
        return if (isGridLayout) {
            canScrollHorizontally && !isInLastRow || canScrollVertically && !fillsLastSpan
        } else {
            canScrollHorizontally && !lastItem
        }
    }

    private fun useBottomPadding(): Boolean {
        return if (isGridLayout) {
            canScrollHorizontally && !fillsLastSpan || canScrollVertically && !isInLastRow
        } else {
            canScrollVertically && !lastItem
        }
    }

}