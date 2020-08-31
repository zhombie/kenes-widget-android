package q19.kenes_widget.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.data.model.Message
import q19.kenes_widget.ui.presentation.adapter.InlineKeyboardAdapterItemDecoration
import q19.kenes_widget.ui.util.buildRippleDrawable
import q19.kenes_widget.util.inflate

internal class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var recyclerView: RecyclerView? = null

    private var keyboardAdapter: KeyboardAdapter? = null

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_keyboard, this)

        recyclerView = view?.findViewById(R.id.recyclerView)

        keyboardAdapter = KeyboardAdapter {
            callback?.onReplyMarkupButtonClicked(it)
        }

        recyclerView?.adapter = keyboardAdapter

        recyclerView?.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        recyclerView?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        recyclerView?.addItemDecoration(InlineKeyboardAdapterItemDecoration(
            context.resources.getDimension(R.dimen.kenes_rounded_border_width),
            context.resources.getDimension(R.dimen.kenes_rounded_border_radius)
        ))
    }

    fun setReplyMarkup(replyMarkup: Message.ReplyMarkup?) {
        keyboardAdapter?.replyMarkup = replyMarkup
    }

    interface Callback {
        fun onReplyMarkupButtonClicked(button: Message.ReplyMarkup.Button)
    }

}


private class KeyboardAdapter(
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
            textView.text = button.text

            itemView.background = buildRippleDrawable(itemView.context)

            itemView.setOnClickListener { callback(button) }
        }

    }

}