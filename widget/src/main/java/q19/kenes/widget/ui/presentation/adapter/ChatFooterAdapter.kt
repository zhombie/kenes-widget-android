package q19.kenes.widget.ui.presentation.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes.widget.core.errors.ViewHolderViewTypeException
import q19.kenes.widget.ui.presentation.model.ChatFooter
import q19.kenes.widget.util.inflate
import q19.kenes.widget.util.removeCompoundDrawables
import q19.kenes.widget.util.showCompoundDrawableOnfLeft
import java.util.*

internal class ChatFooterAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val LAYOUT_FOOTER = R.layout.kenes_cell_footer
        val LAYOUT_FOOTER_FUZZY = R.layout.kenes_cell_footer_fuzzy
    }

    private var data = mutableListOf<ChatFooter>()

    var callback: Callback? = null

    fun showGoToHomeButton() {
        showButton(ChatFooter.Type.GO_TO_HOME)
    }

    fun showSwitchToCallAgentButton() {
        showButton(ChatFooter.Type.SWITCH_TO_CALL_AGENT)
    }

    fun showFuzzyQuestionButtons() {
        showButton(ChatFooter.Type.FUZZY_QUESTION)
    }

    private fun showButton(type: ChatFooter.Type) {
        if (data.isEmpty()) {
            data.add(0, ChatFooter(type))
            notifyItemInserted(0)
        } else {
            if (data.first().type == type) {
                return
            } else {
                data.clear()
                notifyItemRemoved(0)
                data.add(0, ChatFooter(type))
                notifyItemInserted(0)
            }
        }
    }

    fun clear() {
        if (data.isEmpty()) return

        data.clear()
        notifyItemRemoved(0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (data.isNotEmpty()) {
            when (data[position].type) {
                ChatFooter.Type.GO_TO_HOME ->
                    LAYOUT_FOOTER
                ChatFooter.Type.SWITCH_TO_CALL_AGENT, ChatFooter.Type.FUZZY_QUESTION ->
                    LAYOUT_FOOTER_FUZZY
            }
        } else {
            super.getItemViewType(position)
        }
    }

    private fun getItem(position: Int) = data[position]

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = parent.inflate(viewType)
        return when (viewType) {
            LAYOUT_FOOTER -> FooterViewHolder(view)
            LAYOUT_FOOTER_FUZZY -> FuzzyFooterViewHolder(view)
            else -> throw ViewHolderViewTypeException(viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is FooterViewHolder) {
            holder.bind(item)
        } else if (holder is FuzzyFooterViewHolder) {
            holder.bind(item)
        }
    }

    private inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val button = view.findViewById<AppCompatButton>(R.id.button)

        fun bind(chatFooter: ChatFooter) {
            if (chatFooter.type == ChatFooter.Type.GO_TO_HOME) {
                button?.showCompoundDrawableOnfLeft(R.drawable.kenes_selector_arrow_left, 15)
                button?.setText(R.string.kenes_go_to_home)
                button?.setOnClickListener { callback?.onGoToHomeButtonClicked() }
            }
        }
    }

    private inner class FuzzyFooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val button1 = view.findViewById<AppCompatButton>(R.id.button1)
        private val orView = view.findViewById<TextView>(R.id.orView)
        private val button2 = view.findViewById<AppCompatButton>(R.id.button2)

        fun bind(chatFooter: ChatFooter) {
            if (chatFooter.type == ChatFooter.Type.FUZZY_QUESTION) {
                button1?.showCompoundDrawableOnfLeft(R.drawable.kenes_selector_headphones, 15)
                button1?.setText(R.string.kenes_switch_to_operator)
                button1?.setOnClickListener { callback?.onSwitchToCallAgentButtonClicked() }

                orView?.text = itemView.context.getString(R.string.kenes_or).toLowerCase(Locale.getDefault())

                button2?.removeCompoundDrawables()
                button2?.setText(R.string.kenes_register_appeal)
                button2?.setOnClickListener { callback?.onRegisterAppealButtonClicked() }
            } else if (chatFooter.type == ChatFooter.Type.SWITCH_TO_CALL_AGENT) {
                button1?.removeCompoundDrawables()
                button1?.setText(R.string.kenes_switch_to_operator)
                button1?.setOnClickListener { callback?.onSwitchToCallAgentButtonClicked() }

                orView?.text = itemView.context.getString(R.string.kenes_or).toLowerCase(Locale.getDefault())

                button2?.removeCompoundDrawables()
                button2?.setText(R.string.kenes_go_to_home)
                button2?.setOnClickListener { callback?.onGoToHomeButtonClicked() }
            }
        }
    }

    interface Callback {
        fun onGoToHomeButtonClicked()
        fun onSwitchToCallAgentButtonClicked()
        fun onRegisterAppealButtonClicked()
    }

}