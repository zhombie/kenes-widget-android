package q19.kenes.widget.ui.presentation.call.text

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.message.Message
import kz.q19.utils.android.dp2Px
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.KenesMessageInputView
import q19.kenes.widget.ui.components.KenesToolbar
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.common.chat.SpacingItemDecoration
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class TextChatFragment : BaseFragment<TextChatPresenter>(R.layout.fragment_text_chat),
    TextChatView,
    ChatMessagesAdapter.Callback {

    companion object {
        private val TAG = TextChatFragment::class.java.simpleName

        fun newInstance(): TextChatFragment {
            return TextChatFragment()
        }
    }

    // UI Views
    private var toolbar: KenesToolbar? = null
    private var showVideoCallButton: MaterialButton? = null
    private var messagesView: RecyclerView? = null
    private var messageInputView: KenesMessageInputView? = null

    // RecyclerView adapter
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener {
        fun onShowVideoCallScreen()
        fun onHangupCall()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Listener) {
            this.listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
    }

    override fun createPresenter(): TextChatPresenter {
        return injection.provideTextChatPresenter(getCurrentLanguage())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        showVideoCallButton = view.findViewById(R.id.showVideoCallButton)
        messagesView = view.findViewById(R.id.messagesView)
        messageInputView = view.findViewById(R.id.messageInputView)

        setupToolbar()
        setupVideoCallButton()
        setupMessagesView()
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()

        chatMessagesAdapter?.callback = null
        chatMessagesAdapter = null
    }

    private fun setupToolbar() {
        toolbar?.setRightButtonEnabled(true)
        toolbar?.setRightButtonBackgroundTint(R.color.kenes_soft_red)
        toolbar?.setRightButtonIcon(R.drawable.ic_phone)
        toolbar?.setRightButtonIconTint(R.color.kenes_white)
        toolbar?.setRightButtonOnClickListener {
            listener?.onHangupCall()
        }

        toolbar?.showImage(R.drawable.ic_user)
        toolbar?.setTitle("Имя оператора")
        toolbar?.setSubtitle("Ожидание...")
        toolbar?.reveal()
    }

    private fun setupVideoCallButton() {
        showVideoCallButton?.setOnClickListener {
            listener?.onShowVideoCallScreen()
        }
    }

    private fun setupMessagesView() {
        messagesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesAdapter = ChatMessagesAdapter(this)
        messagesView?.addItemDecoration(SpacingItemDecoration(5F.dp2Px()))
        messagesView?.adapter = chatMessagesAdapter

        messagesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        messageInputView?.setCallback(object : KenesMessageInputView.Callback {
            override fun onNewMediaSelection() {
            }

            override fun onSendTextMessage(message: String?) {
                presenter.onSendTextMessage(message)
            }
        })
    }

    fun showCallAgentInfo(fullName: String, photoUrl: String?) {
        activity?.runOnUiThread {
            toolbar?.setImageContentPadding(0)
            toolbar?.showImage(photoUrl)

            toolbar?.setTitle(fullName)
            toolbar?.setSubtitle("Оператор")

            toolbar?.reveal()
        }
    }

    fun onNewChatMessage(message: Message) {
        activity?.runOnUiThread {
            presenter.onNewChatMessage(message)
        }
    }

    /**
     * [ChatMessagesAdapter.Callback] implementation
     */

    override fun onUrlInTextClicked(url: String) {
        toast("url: $url")
    }

    override fun onMessageLongClicked(text: String) {
        toast("copy: $text")
    }

    /**
     * [TextChatView] implementation
     */

    override fun showNewMessage(message: Message) {
        Logger.debug(TAG, "showNewMessage() -> $message")

        activity?.runOnUiThread {
            chatMessagesAdapter?.addNewMessage(message)
        }
    }

    override fun clearMessageInput() {
        activity?.runOnUiThread {
            messageInputView?.clearInputViewText()
        }
    }

}