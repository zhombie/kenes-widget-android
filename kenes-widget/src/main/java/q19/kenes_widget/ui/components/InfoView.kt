package q19.kenes_widget.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.Configs
import q19.kenes_widget.model.Language
import q19.kenes_widget.util.showCompoundDrawableOnfLeft

internal class InfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val recyclerView: RecyclerView

    private val adapter: MenuAdapter

    var callback: Callback? = null

    init {
        val view = inflate(context, R.layout.kenes_view_info, this)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = MenuAdapter(object : MenuAdapter.Callback {
            override fun onPhoneNumberClicked(phoneNumber: String) {
                callback?.onPhoneNumberClicked(phoneNumber)
            }

            override fun onSocialClicked(contact: Configs.Contact) {
                callback?.onSocialClicked(contact)
            }

            override fun onLanguageChangeClicked(language: Language) {
                callback?.onLanguageChangeClicked(language)
            }
        })
        recyclerView.addItemDecoration(MenuAdapter.ItemDecoration())
        recyclerView.adapter = adapter
    }

    fun setContacts(contacts: List<Configs.Contact>) {
        adapter.contacts = contacts
        adapter.notifyDataSetChanged()
    }

    fun setPhones(phones: List<String>) {
        adapter.phones = phones
        adapter.notifyDataSetChanged()
    }

    fun setLanguage(language: Language) {
        adapter.language = language
        adapter.notifyDataSetChanged()
    }

    interface Callback {
        fun onPhoneNumberClicked(phoneNumber: String)
        fun onSocialClicked(contact: Configs.Contact)
        fun onLanguageChangeClicked(language: Language)
    }

}


private class MenuAdapter(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_MENU = R.layout.kenes_cell_menu
    }

    var phones: List<String> = emptyList()
    var contacts: List<Configs.Contact> = emptyList()
    var language: Language = Language.DEFAULT

    override fun getItemCount(): Int = contacts.size + phones.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(LAYOUT_MENU, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            if (position < phones.size) {
                holder.bind(phones[position])
            } else if (position >= phones.size && position < phones.size + contacts.size) {
                val index = position - phones.size
                holder.bind(contacts[index])
            } else if (position == phones.size + contacts.size) {
                holder.bind(language)
            }
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textView: TextView? = null
        private var subView: TextView? = null

        init {
            textView = view.findViewById(R.id.textView)
            subView = view.findViewById(R.id.subView)
        }

        fun bind(language: Language) {
            textView?.setText(R.string.kenes_change_language)

            subView?.text = language.value
            subView?.visibility = View.VISIBLE

            textView?.showCompoundDrawableOnfLeft(R.drawable.kenes_ic_globe, 35)

            itemView.setOnClickListener { callback.onLanguageChangeClicked(language) }
        }

        fun bind(phoneNumber: String) {
            subView?.visibility = View.GONE

            if (phoneNumber.isBlank()) {
                textView?.visibility = View.GONE
            } else {
                textView?.text = phoneNumber
                textView?.showCompoundDrawableOnfLeft(R.drawable.kenes_ic_phone_blue, 35)

                textView?.visibility = View.VISIBLE

                itemView.setOnClickListener { callback.onPhoneNumberClicked(phoneNumber) }
            }
        }

        fun bind(contact: Configs.Contact) {
            subView?.visibility = View.GONE

            if (contact.social == null || contact.social?.title.isNullOrBlank()) {
                textView?.visibility = View.GONE
            } else {
                textView?.text = itemView.context.getString(R.string.kenes_chat_bot, contact.social?.title)
                textView?.showCompoundDrawableOnfLeft(contact.social?.icon ?: 0, 35)

                textView?.visibility = View.VISIBLE

                itemView.setOnClickListener { callback.onSocialClicked(contact) }
            }
        }
    }

    interface Callback {
        fun onPhoneNumberClicked(phoneNumber: String)
        fun onSocialClicked(contact: Configs.Contact)
        fun onLanguageChangeClicked(language: Language)
    }

    class ItemDecoration : RecyclerView.ItemDecoration() {

        private var paint: Paint = Paint()

        init {
            paint.color = Color.parseColor("#BFF3F3F3")
            paint.strokeWidth = 2F
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val startX = parent.paddingStart + 100
            val stopX = parent.width - parent.paddingEnd - 35

            val childCount = parent.childCount
            for (i in 0 until childCount - 1) {
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

}