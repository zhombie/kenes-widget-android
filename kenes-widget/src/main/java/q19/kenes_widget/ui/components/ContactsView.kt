package q19.kenes_widget.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.data.model.Configs
import q19.kenes_widget.data.model.Language
import q19.kenes_widget.util.inflate
import q19.kenes_widget.util.loadCircleImage
import q19.kenes_widget.util.setOverscrollColor

@Deprecated("Not used anymore")
internal class ContactsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val recyclerView: RecyclerView

    private var adapter: InfoBlocksAdapter? = null

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_contacts, this)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.setOverscrollColor(R.color.kenes_light_blue)
        recyclerView.addItemDecoration(InfoBlocksAdapterItemDecoration(context))
    }

    fun show(infoBlocks: List<Configs.InfoBlock>, language: Language) {
        adapter = InfoBlocksAdapter(language) {
            callback?.onInfoBlockItemClicked(it)
        }
        recyclerView.adapter = adapter
        adapter?.infoBlocks = infoBlocks
    }

    interface Callback {
        fun onInfoBlockItemClicked(item: Configs.Item)
    }

}

private class InfoBlocksAdapter(
    private val language: Language,
    private val callback: (item: Configs.Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_INFO_BLOCK = R.layout.kenes_cell_info_block
    }

    var infoBlocks = emptyList<Configs.InfoBlock>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int) = infoBlocks[position]

    override fun getItemCount(): Int = infoBlocks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(LAYOUT_INFO_BLOCK))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView = view.findViewById<TextView>(R.id.titleView)
        private val descriptionView = view.findViewById<TextView>(R.id.descriptionView)
        private val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        private var adapter: InfoBlockRowsAdapter? = null

        init {
            titleView.visibility = View.GONE
            descriptionView.visibility = View.GONE
            recyclerView.visibility = View.GONE

            recyclerView.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            adapter = InfoBlockRowsAdapter(language) {
                callback(it)
            }
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(InfoBlockRowsAdapterItemDecoration(itemView.context))
        }

        fun bind(infoBlock: Configs.InfoBlock) {
            val title = infoBlock.title.get(language)
            val description = infoBlock.description.get(language)
            val items = infoBlock.items

            if (title.isNotBlank()) {
                titleView.text = title
                titleView.visibility = View.VISIBLE
            } else {
                titleView.visibility = View.GONE
            }

            if (description.isNotBlank()) {
                descriptionView.text = description
                descriptionView.visibility = View.VISIBLE
            } else {
                descriptionView.visibility = View.GONE
            }

            if (!items.isNullOrEmpty()) {
                adapter?.items = items
                recyclerView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.GONE
            }
        }
    }

}

private class InfoBlocksAdapterItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private var verticalSpacing: Int =
        context.resources.getDimensionPixelOffset(R.dimen.kenes_info_block_vertical_spacing)

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

private class InfoBlockRowsAdapter(
    private val language: Language,
    private val callback: (item: Configs.Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_INFO_BLOCK_ROW = R.layout.kenes_cell_info_block_row
    }

    var items = emptyList<Configs.Item>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private fun getItem(position: Int) = items[position]

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(parent.inflate(LAYOUT_INFO_BLOCK_ROW))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(getItem(position))
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView = view.findViewById<ImageView>(R.id.iconView)
        private val textView = view.findViewById<TextView>(R.id.textView)
        private val descriptionView = view.findViewById<TextView>(R.id.descriptionView)

        init {
            iconView.visibility = View.GONE
            descriptionView.visibility = View.GONE
        }

        fun bind(item: Configs.Item) {
            val icon = item.icon
            val description = item.description.get(language)

            if (!icon.isNullOrBlank()) {
                iconView.loadCircleImage(item.icon)
                iconView.visibility = View.VISIBLE
            } else {
                iconView.visibility = View.GONE
            }

            textView.text = item.text

            if (description.isNotBlank()) {
                descriptionView.text = description
                descriptionView.visibility = View.VISIBLE
            } else {
                descriptionView.visibility = View.GONE
            }

            itemView.setOnClickListener { callback(item) }
        }
    }
}

private class InfoBlockRowsAdapterItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val horizonalSpacing =
        context.resources.getDimensionPixelOffset(R.dimen.kenes_info_block_horizontal_spacing)

    private val paint: Paint = Paint()

    init {
        paint.color = Color.parseColor("#BFF3F3F3")
        paint.strokeWidth = 2F
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val startX = parent.paddingStart + horizonalSpacing
        val stopX = parent.width - horizonalSpacing

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val y = child.bottom + params.bottomMargin
            c.drawLine(startX.toFloat(), y.toFloat(), stopX.toFloat(), y.toFloat(), paint)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.bottom = 2
    }

}