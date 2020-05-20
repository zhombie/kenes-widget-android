package q19.kenes_widget.adapter

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R

internal class ChatAdapterItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private var verticalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.kenes_message_vertical_spacing)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val adapter = parent.adapter as? ChatAdapter?
        val position = parent.layoutManager?.getPosition(view)

        if (adapter != null && position != null) {
            when (adapter.getItemViewType(position)) {
                ChatAdapter.LAYOUT_NOTIFICATION -> {
                    outRect.top = verticalSpacing
                    outRect.bottom = verticalSpacing
                }
                else ->
                    outRect.bottom = verticalSpacing * 2
            }
        }
    }
}