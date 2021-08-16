package q19.kenes.widget.ui.presentation.call.text

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.q19.domain.model.message.Message
import q19.kenes.widget.ui.components.MessageInputView
import q19.kenes.widget.ui.presentation.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes.widget.util.loadImage
import q19.kenes.widget.util.picasso.CircleTransformation
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
    private var toolbar: LinearLayout? = null
    private var imageView: ShapeableImageView? = null
    private var titleView: MaterialTextView? = null
    private var subtitleView: MaterialTextView? = null
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
        imageView = view.findViewById(R.id.imageView)
        titleView = view.findViewById(R.id.titleView)
        subtitleView = view.findViewById(R.id.subtitleView)
        videoCallButton = view.findViewById(R.id.videoCallButton)
        messagesView = view.findViewById(R.id.messagesView)
        messageInputView = view.findViewById(R.id.messageInputView)

        setupVideoCallButton()
        setupMessagesView()
    }

    override fun onDestroy() {
        super.onDestroy()

        chatMessagesAdapter = null

        listener = null
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
        imageView?.loadImage(photoUrl, transformation = CircleTransformation())
        titleView?.text = fullName
        subtitleView?.text = "Оператор"
    }

    fun onNewMessage(message: Message) {
        presenter.onNewMessage(message)
    }

    /**
     * [TextChatView] implementation
     */

    override fun showNewMessage(message: Message) {
        activity?.runOnUiThread {
            chatMessagesAdapter?.addNewMessage(message)
        }
    }

    override fun clearMessageInput() {
        messageInputView?.clearInputViewText()
    }

}