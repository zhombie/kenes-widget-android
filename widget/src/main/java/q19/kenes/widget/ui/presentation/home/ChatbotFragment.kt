package q19.kenes.widget.ui.presentation.home

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EdgeEffect
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.message.Message
import kz.q19.utils.android.clipboardManager
import kz.q19.utils.html.HTMLCompat
import kz.q19.utils.keyboard.hideKeyboard
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.domain.model.Element
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.components.MessageInputView
import q19.kenes.widget.ui.components.ProgressView
import q19.kenes.widget.ui.presentation.HomeFragmentDelegate
import q19.kenes.widget.ui.presentation.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.chat.ChatMessagesHeaderAdapter
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes.widget.util.AlertDialogBuilder
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class ChatbotFragment : BaseFragment<ChatbotPresenter>(R.layout.fragment_chatbot), ChatbotView,
    HomeFragmentDelegate {

    companion object {
        private val TAG = ChatbotFragment::class.java.simpleName

        fun newInstance(): ChatbotFragment {
            val fragment = ChatbotFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    // UI Views
    private var responsesView: RecyclerView? = null
    private var progressView: ProgressView? = null
    private var chatView: LinearLayout? = null
    private var peekView: LinearLayout? = null
    private var toggleButton: MaterialButton? = null
    private var closeButton: MaterialButton? = null
    private var messagesView: RecyclerView? = null
    private var messageInputView: MessageInputView? = null

    // RecyclerView adapter
    private var concatAdapter: ConcatAdapter? = null

    private var responseGroupsAdapter: ResponseGroupsAdapter? = null

    private var chatMessagesHeaderAdapter: ChatMessagesHeaderAdapter? = null
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null

    // onBackPressed() dispatcher for Fragment
    private var onBackPressedDispatcherCallback: OnBackPressedCallback? = null

    interface Listener {
        fun onResponsesViewScrolled(scrollYPosition: Int)
        fun onBottomSheetSlide(slideOffset: Float)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)

        onBackPressedDispatcherCallback = activity?.onBackPressedDispatcher?.addCallback {
            if (presenter.onBackPressed()) {
                isEnabled = false
                activity?.onBackPressed()
            }
        }
    }

    override fun createPresenter(): ChatbotPresenter {
        return injection.provideChatbotPresenter(getCurrentLanguage())
    }

    override fun onResume() {
        super.onResume()

        Logger.debug(TAG, "onResume()")

        if (onBackPressedDispatcherCallback == null) {
            onBackPressedDispatcherCallback = activity?.onBackPressedDispatcher?.addCallback(this) {
                if (presenter.onBackPressed()) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        } else {
            onBackPressedDispatcherCallback?.isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        responsesView = view.findViewById(R.id.responsesView)
        progressView = view.findViewById(R.id.progressView)
        chatView = view.findViewById(R.id.chatView)
        peekView = view.findViewById(R.id.peekView)
        toggleButton = view.findViewById(R.id.toggleButton)
        closeButton = view.findViewById(R.id.closeButton)
        messagesView = view.findViewById(R.id.messagesView)
        messageInputView = view.findViewById(R.id.messageInputView)

        setupResponsesView()
        setupBottomSheet()
        setupMessagesView()
    }

    override fun onPause() {
        super.onPause()

        Logger.debug(TAG, "onPause()")

        onBackPressedDispatcherCallback?.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()

        listener = null

        responseGroupsAdapter?.setCallback(null)
        responseGroupsAdapter = null

        messageInputView?.setCallback(null)
        messageInputView = null

        chatMessagesAdapter?.let { concatAdapter?.removeAdapter(it) }
        chatMessagesAdapter = null

        chatMessagesHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        chatMessagesHeaderAdapter = null

        responsesView?.clearOnScrollListeners()

        onBackPressedDispatcherCallback?.remove()
        onBackPressedDispatcherCallback = null
    }

    private fun setupResponsesView() {
        responseGroupsAdapter = ResponseGroupsAdapter()
        responseGroupsAdapter?.setCallback(object : ResponseGroupsAdapter.Callback {
            override fun onBackPressed(element: Element) {
                presenter.onBackPressed()
            }

            override fun onMenuButtonClicked() {
                AlertDialogBuilder(requireContext())
                    .setTitle(R.string.menu)
                    .setItems(
                        arrayOf(
                            getString(R.string.copy),
                            getString(R.string.share)
                        )
                    ) { dialog, which ->
                        dialog.dismiss()
                        when (which) {
                            0 -> presenter.onCopyResponseText()
                            1 -> presenter.onShareResponse()
                        }
                    }
                    .show()
            }

            override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
                presenter.onResponseGroupClicked(responseGroup)
            }

            override fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
                presenter.onResponseGroupChildClicked(child)
            }
        })
        responsesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        responsesView?.itemAnimator = null
        responsesView?.adapter = responseGroupsAdapter

        responsesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        responsesView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                listener?.onResponsesViewScrolled(recyclerView.computeVerticalScrollOffset())
            }
        })
    }

    private fun setupBottomSheet() {
        closeButton?.alpha = 0F

        chatView?.let {
            bottomSheetBehavior = BottomSheetBehavior.from(it)
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                private val peekHeight =
                    requireContext().resources.getDimensionPixelOffset(R.dimen.bottom_sheet_peek_height)

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    Logger.debug(TAG, "onStateChanged() -> $slideOffset")

                    val reverseOffset = 1F - slideOffset

                    closeButton?.alpha = slideOffset

                    peekView?.alpha = reverseOffset

                    peekView?.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = (peekHeight * reverseOffset).roundToInt()
                    }

                    listener?.onBottomSheetSlide(slideOffset)
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Logger.debug(TAG, "onStateChanged() -> $newState")

                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        activity?.hideKeyboard()
                    }

                    presenter.onBottomSheetStateChanged(newState == BottomSheetBehavior.STATE_EXPANDED)
                }
            })
        }

        toggleButton?.setOnClickListener {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        closeButton?.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setupMessagesView() {
        messagesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesHeaderAdapter = ChatMessagesHeaderAdapter()
        chatMessagesAdapter = ChatMessagesAdapter()
        concatAdapter = ConcatAdapter(chatMessagesAdapter, chatMessagesHeaderAdapter)
        messagesView?.adapter = concatAdapter

        messagesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        messageInputView?.setCallback(object : MessageInputView.Callback {
            override fun onNewMediaSelection() {
                // TODO: Implement media selection with certain permissions
            }

            override fun onSendTextMessage(message: String?) {
                presenter.onSendTextMessage(message)
            }
        })
    }

    /**
     * [ChatbotView] implementation
     */

    override fun hideLoadingIndicator() {
        progressView?.hide()
    }

    override fun showLoadingIndicator() {
        progressView?.show()
    }

    override fun showResponses(nestables: List<Nestable>) {
        responseGroupsAdapter?.submitList(nestables)
    }

    override fun showNewChatMessage(message: Message) {
        activity?.runOnUiThread {
            chatMessagesAdapter?.addNewMessage(message)
        }
    }

    override fun copyHTMLText(label: String, text: CharSequence?, htmlText: String) {
        if (text.isNullOrBlank()) {
            context?.clipboardManager?.setPrimaryClip(
                ClipData.newHtmlText(label, HTMLCompat.fromHtml(htmlText), htmlText)
            )
        } else {
            context?.clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
        }

        toast(R.string.copy)
    }

    override fun share(title: String, text: CharSequence?, htmlText: String) {
        try {
            val share = Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND

                type = "text/plain"

                putExtra(Intent.EXTRA_TITLE, title)

                if (text.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_HTML_TEXT, htmlText)
                } else {
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            }, null)
            startActivity(share)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun toggleBottomSheet() {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun clearMessageInput() {
        messageInputView?.clearInputViewText()
    }

    override fun hideChatMessagesHeader() {
        chatMessagesHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        chatMessagesHeaderAdapter = null
    }

    override fun showNoResponsesFoundMessage() {
        toast(R.string.no_knowledge_base_available_response)
    }

    /**
     * [HomeFragmentDelegate] implementation
     */

    override fun onScreenRenavigate() {
        Logger.debug(TAG, "onScreenRenavigate()")

        with(responsesView?.layoutManager) {
            if (this is LinearLayoutManager) {
                if (findFirstCompletelyVisibleItemPosition() == 0) {
                    presenter.onResetDataRequested()
                } else {
                    responsesView?.smoothScrollToPosition(0)
                }
            }
        }
    }

}