package q19.kenes_widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ChatAdapterItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private var verticalSpacing: Int = context.resources.getDimensionPixelOffset(R.dimen.message_vertical_spacing)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.bottom = verticalSpacing
    }
}