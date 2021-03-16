package q19.kenes.widget.ui.components

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
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import kz.q19.utils.textview.showCompoundDrawableOnLeft
import q19.kenes_widget.R

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
            override fun onPhoneNumberClicked(phoneNumber: Configs.Contacts.PhoneNumber) {
                callback?.onPhoneNumberClicked(phoneNumber.value)
            }

            override fun onSocialClicked(social: Configs.Contacts.Social) {
                callback?.onSocialClicked(social)
            }

            override fun onLanguageChangeClicked(language: Language) {
                callback?.onLanguageChangeClicked(language)
            }
        })
        recyclerView.addItemDecoration(MenuAdapter.ItemDecoration())
        recyclerView.adapter = adapter
    }

    fun setContacts(socials: List<Configs.Contacts.Social>) {
        adapter.socials = socials
        adapter.notifyDataSetChanged()
    }

    fun setPhones(phoneNumbers: List<Configs.Contacts.PhoneNumber>) {
        adapter.phoneNumbers = phoneNumbers
        adapter.notifyDataSetChanged()
    }

    fun setLanguage(language: Language) {
        adapter.language = language
        adapter.notifyDataSetChanged()
    }

    interface Callback {
        fun onPhoneNumberClicked(phoneNumber: String)
        fun onSocialClicked(contact: Configs.Contacts.Social)
        fun onLanguageChangeClicked(language: Language)
    }

}


private class MenuAdapter constructor(
    private val callback: Callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val LAYOUT_MENU = R.layout.kenes_cell_menu
    }

    var socials = emptyList<Configs.Contacts.Social>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var phoneNumbers = emptyList<Configs.Contacts.PhoneNumber>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var language: Language = Language.DEFAULT

    private fun getSocial(position: Int): Configs.Contacts.Social {
        return socials[position]
    }

    private fun getPhoneNumber(position: Int): Configs.Contacts.PhoneNumber {
        return phoneNumbers[position]
    }

    override fun getItemCount(): Int = socials.size + phoneNumbers.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(LAYOUT_MENU, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            if (position < socials.size) {
                holder.bind(getSocial(position))
            } else if (position >= phoneNumbers.size && position < socials.size + phoneNumbers.size) {
                val index = position - socials.size
                val phoneNumber = phoneNumbers[index]
                holder.bind(phoneNumber)
            } else if (position == socials.size + phoneNumbers.size) {
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

            subView?.text = language.representation
            subView?.visibility = View.VISIBLE

            textView?.showCompoundDrawableOnLeft(R.drawable.kenes_ic_globe, 35)

            itemView.setOnClickListener { callback.onLanguageChangeClicked(language) }
        }

        fun bind(phoneNumber: Configs.Contacts.PhoneNumber) {
            subView?.visibility = View.GONE

            if (phoneNumber.value.isBlank()) {
                textView?.visibility = View.GONE
            } else {
                textView?.text = phoneNumber.value
                textView?.showCompoundDrawableOnLeft(R.drawable.kenes_ic_phone_blue, 35)

                textView?.visibility = View.VISIBLE

                itemView.setOnClickListener { callback.onPhoneNumberClicked(phoneNumber) }
            }
        }

        fun bind(social: Configs.Contacts.Social) {
            subView?.visibility = View.GONE

            var title: String? = null
            var iconRes: Int? = null
            when (social.id) {
                Configs.Contacts.Social.Id.FACEBOOK -> {
                    iconRes = R.drawable.kenes_ic_messenger
                    title = "Facebook"
                }
                Configs.Contacts.Social.Id.TELEGRAM -> {
                    iconRes = R.drawable.kenes_ic_telegram
                    title = "Telegram"
                }
                Configs.Contacts.Social.Id.TWITTER -> {
                    iconRes = R.drawable.kenes_ic_twitter
                    title = "Twitter"
                }
                Configs.Contacts.Social.Id.VK -> {
                    iconRes = R.drawable.kenes_ic_vk
                    title = "ВКонтакте"
                }
            }

            textView?.text = itemView.context.getString(R.string.kenes_chat_bot, title)
            textView?.showCompoundDrawableOnLeft(iconRes, 35)

            textView?.visibility = View.VISIBLE

            itemView.setOnClickListener { callback.onSocialClicked(social) }
        }
    }

    interface Callback {
        fun onPhoneNumberClicked(phoneNumber: Configs.Contacts.PhoneNumber)
        fun onSocialClicked(social: Configs.Contacts.Social)
        fun onLanguageChangeClicked(language: Language)
    }

    class ItemDecoration : RecyclerView.ItemDecoration() {

        private val paint: Paint = Paint()

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