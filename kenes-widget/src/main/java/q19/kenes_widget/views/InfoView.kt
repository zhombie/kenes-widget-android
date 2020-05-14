package q19.kenes_widget.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import q19.kenes_widget.R
import q19.kenes_widget.model.Configs
import q19.kenes_widget.model.Language

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
                holder.bind(contacts[contacts.size - position])
            } else {
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

            subView?.visibility = View.VISIBLE
            subView?.text = language.value

            textView?.showCompoundDrawableOnfLeft(R.drawable.kenes_ic_globe)

            itemView.setOnClickListener { callback.onLanguageChangeClicked(language) }
        }

        fun bind(phoneNumber: String) {
            subView?.visibility = View.GONE

            textView?.text = phoneNumber

            textView?.showCompoundDrawableOnfLeft(R.drawable.kenes_ic_phone_blue)

            itemView.setOnClickListener { callback.onPhoneNumberClicked(phoneNumber) }
        }

        fun bind(contact: Configs.Contact) {
            subView?.visibility = View.GONE

            textView?.text = itemView.context.getString(R.string.kenes_chat_bot, contact.social?.title)

            textView?.showCompoundDrawableOnfLeft(contact.social?.icon ?: 0)

            itemView.setOnClickListener { callback.onSocialClicked(contact) }
        }

        private fun TextView.showCompoundDrawableOnfLeft(@DrawableRes drawableRes: Int) {
            setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0)
            compoundDrawablePadding = 35
        }
    }

    interface Callback {
        fun onPhoneNumberClicked(phoneNumber: String)
        fun onSocialClicked(contact: Configs.Contact)
        fun onLanguageChangeClicked(language: Language)
    }

}