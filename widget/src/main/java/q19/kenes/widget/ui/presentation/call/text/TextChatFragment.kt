package q19.kenes.widget.ui.presentation.call.text

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.message.Message
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.ui.components.MessageInputView
import q19.kenes.widget.ui.components.Toolbar
import q19.kenes.widget.ui.presentation.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class TextChatFragment : BaseFragment<TextChatPresenter>(R.layout.fragment_text_chat),
    TextChatView {

    companion object {
        private val TAG = TextChatFragment::class.java.simpleName

        fun newInstance(): TextChatFragment {
            return TextChatFragment()
        }
    }

    // UI Views
    private var toolbar: Toolbar? = null
    private var videoCallButton: MaterialButton? = null
    private var messagesView: RecyclerView? = null
    private var messageInputView: MessageInputView? = null

    // RecyclerView adapter
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    // Fragment communication
    private var listener: (() -> Unit)? = null

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
        videoCallButton = view.findViewById(R.id.videoCallButton)
        messagesView = view.findViewById(R.id.messagesView)
        messageInputView = view.findViewById(R.id.messageInputView)

        setupToolbar()
        setupVideoCallButton()
        setupMessagesView()
    }

    override fun onDestroy() {
        super.onDestroy()

        chatMessagesAdapter = null

        listener = null
    }

    private fun setupToolbar() {
        toolbar?.setRightButtonEnabled(true)
        toolbar?.setRightButtonBackgroundTint(R.color.kenes_soft_red)
        toolbar?.setRightButtonIcon(R.drawable.ic_phone)
        toolbar?.setRightButtonIconTint(R.color.kenes_white)
        toolbar?.setRightButtonOnClickListener {
            if (parentFragment is DialogFragment) {
                (parentFragment as DialogFragment).dialog?.onBackPressed()
            }
        }

        toolbar?.showImage(R.drawable.ic_user)
        toolbar?.setTitle("Имя оператора")
        toolbar?.setSubtitle("Ожидание...")
        toolbar?.reveal()
    }

    private fun setupVideoCallButton() {
        videoCallButton?.setOnClickListener {
            listener?.invoke()
        }
    }

    private fun setupMessagesView() {
        messagesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesAdapter = ChatMessagesAdapter()
        messagesView?.adapter = chatMessagesAdapter

        messagesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        messageInputView?.setCallback(object : MessageInputView.Callback {
            override fun onNewMediaSelection() {
            }

            override fun onSendTextMessage(message: String?) {
                presenter.onSendTextMessage(message)
            }
        })
    }

    fun setListener(listener: (() -> Unit)?) {
        this.listener = listener
    }

    fun showCallAgentInfo(fullName: String, photoUrl: String?) {
        activity?.runOnUiThread {
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