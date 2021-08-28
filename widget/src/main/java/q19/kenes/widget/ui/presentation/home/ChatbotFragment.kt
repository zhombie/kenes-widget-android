package q19.kenes.widget.ui.presentation.home

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
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
import kz.q19.utils.android.dp2Px
import kz.q19.utils.html.HTMLCompat
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.domain.model.Element
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.components.KenesMessageInputView
import q19.kenes.widget.ui.components.KenesProgressView
import q19.kenes.widget.ui.presentation.HomeScreenDelegate
import q19.kenes.widget.ui.presentation.common.BottomSheetState
import q19.kenes.widget.ui.presentation.common.HomeFragment
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesHeaderAdapter
import q19.kenes.widget.ui.presentation.common.chat.SpacingItemDecoration
import q19.kenes.widget.util.AlertDialogBuilder
import q19.kenes.widget.util.hideKeyboardCompat
import q19.kenes_widget.R
import kotlin.math.roundToInt

internal class ChatbotFragment : HomeFragment<ChatbotPresenter>(R.layout.fragment_chatbot),
    ChatbotView,
    HomeScreenDelegate,
    ChatMessagesAdapter.Callback {

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
    private var progressView: KenesProgressView? = null
    private var chatView: LinearLayout? = null
    private var peekView: LinearLayout? = null
    private var toggleButton: MaterialButton? = null
    private var messagesView: RecyclerView? = null
    private var messageInputView: KenesMessageInputView? = null

    // RecyclerView adapter
    private var concatAdapter: ConcatAdapter? = null

    private var responseGroupsAdapter: ResponseGroupsAdapter? = null

    private var chatMessagesHeaderAdapter: ChatMessagesHeaderAdapter? = null
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var bottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback? = null

    // onBackPressed() dispatcher for Fragment
    private var onBackPressedDispatcherCallback: OnBackPressedCallback? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener : HomeFragment.Listener {
        fun onBottomSheetSlide(slideOffset: Float) {}
        fun onBottomSheetStateChanged(state: BottomSheetState) {}
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

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()

        bottomSheetBehaviorCallback?.let { bottomSheetBehavior?.removeBottomSheetCallback(it) }
        bottomSheetBehaviorCallback = null
        bottomSheetBehavior = null

        responseGroupsAdapter?.setCallback(null)
        responseGroupsAdapter = null

        messageInputView?.setCallback(null)
        messageInputView = null

        chatMessagesAdapter?.let { concatAdapter?.removeAdapter(it) }
        chatMessagesAdapter?.callback = null
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
                listener?.onVerticalScroll(recyclerView.computeVerticalScrollOffset())
            }
        })
    }

    private fun setupBottomSheet() {
        chatView?.let { view ->
            bottomSheetBehavior = BottomSheetBehavior.from(view)
            bottomSheetBehavior?.isDraggable = false

            bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
                private val peekHeight =
                    requireContext().resources.getDimensionPixelOffset(R.dimen.bottom_sheet_peek_height)

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                    Logger.debug(TAG, "onStateChanged() -> $slideOffset")

                    val reverseOffset = 1F - slideOffset

                    peekView?.alpha = reverseOffset

                    peekView?.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = (peekHeight * reverseOffset).roundToInt()
                    }

                    listener?.onBottomSheetSlide(slideOffset)
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Logger.debug(TAG, "onStateChanged() -> $newState")

                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        this@ChatbotFragment.view?.hideKeyboardCompat()
                    }

                    with(BottomSheetState.from(newState)) {
                        if (this == null) return@with

                        presenter.onBottomSheetStateChanged(this)

                        listener?.onBottomSheetStateChanged(this)
                    }
                }
            }

            bottomSheetBehaviorCallback?.let {
                bottomSheetBehavior?.addBottomSheetCallback(it)
            }
        }

        toggleButton?.setOnClickListener {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupMessagesView() {
        messagesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesHeaderAdapter = ChatMessagesHeaderAdapter()
        chatMessagesAdapter = ChatMessagesAdapter(this)
        messagesView?.addItemDecoration(SpacingItemDecoration(5F.dp2Px()))
        concatAdapter = ConcatAdapter(chatMessagesAdapter, chatMessagesHeaderAdapter)
        messagesView?.adapter = concatAdapter

        messagesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        messageInputView?.setCallback(object : KenesMessageInputView.Callback {
            override fun onNewMediaSelection() {
                // TODO: Implement media selection with certain permissions
            }

            override fun onSendTextMessage(message: String?) {
                presenter.onSendTextMessage(message)
            }
        })

        messageInputView?.setOnTextChangedListener { s, _, _, _ ->
            if (s.isNullOrBlank()) {
                messageInputView?.setSendMessageButtonEnabled(false)
            } else {
                messageInputView?.setSendMessageButtonEnabled(true)
            }
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

    override fun scrollToBottom() {
        messagesView?.smoothScrollToPosition(0)
    }

    override fun collapseBottomSheet() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun expandBottomSheet() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
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
     * [HomeScreenDelegate] implementation
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