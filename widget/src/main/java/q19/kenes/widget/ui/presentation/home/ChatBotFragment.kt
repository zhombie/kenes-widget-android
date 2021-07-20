package q19.kenes.widget.ui.presentation.home

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kz.q19.utils.android.clipboardManager
import kz.q19.utils.html.HTMLCompat
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.data.local.Database
import q19.kenes.widget.domain.model.Element
import q19.kenes.widget.domain.model.Nestable
import q19.kenes.widget.domain.model.ResponseGroup
import q19.kenes.widget.ui.components.MessageInputView
import q19.kenes.widget.ui.components.ProgressView
import q19.kenes.widget.ui.presentation.HomeFragmentDelegate
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes_widget.R

internal class ChatBotFragment : BaseFragment(R.layout.fragment_chatbot), ChatBotView,
    HomeFragmentDelegate {

    companion object {
        private val TAG = ChatBotFragment::class.java.simpleName

        fun newInstance(): ChatBotFragment {
            val fragment = ChatBotFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }


    // (MVP) Presenter
    private var presenter: ChatBotPresenter? = null

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
    private var adapter: ResponseGroupsAdapter? = null

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null

    private var onBackPressedDispatcherCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ChatBotPresenter(Database.getInstance(requireContext()))
        presenter?.attachView(this)

        onBackPressedDispatcherCallback = activity?.onBackPressedDispatcher?.addCallback {
            if (presenter?.onGoBackButtonClicked() == true) {
                isEnabled = false
                activity?.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
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

    override fun onDestroy() {
        super.onDestroy()

        onBackPressedDispatcherCallback?.remove()
        onBackPressedDispatcherCallback = null

        presenter?.detachView()
        presenter = null
    }

    private fun setupResponsesView() {
        adapter = ResponseGroupsAdapter(object : ResponseGroupsAdapter.Callback {
            override fun onGoBackButtonClicked(element: Element) {
                presenter?.onGoBackButtonClicked(element)
            }

            override fun onMenuButtonClicked() {
                MaterialAlertDialogBuilder(requireContext())
                    .setItems(
                        arrayOf(
                            getString(R.string.copy),
                            getString(R.string.share)
                        )
                    ) { dialog, which ->
                        dialog.dismiss()
                        when (which) {
                            0 -> presenter?.onCopyText()
                            1 -> presenter?.onShare()
                        }
                    }
                    .show()
            }

            override fun onResponseGroupClicked(responseGroup: ResponseGroup) {
                presenter?.onResponseGroupClicked(responseGroup)
            }

            override fun onResponseGroupChildClicked(child: ResponseGroup.Child) {
                presenter?.onResponseGroupChildClicked(child)
            }
        })
        responsesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        responsesView?.itemAnimator = null
        responsesView?.adapter = adapter
    }

    private fun setupBottomSheet() {
        chatView?.let {
            bottomSheetBehavior = BottomSheetBehavior.from(it)

            bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    Logger.debug(TAG, "onStateChanged() -> $slideOffset")

                    val alpha = 1F - slideOffset
                    peekView?.alpha = alpha
                    if (alpha <= 0F) {
                        peekView?.visibility = View.INVISIBLE
                    } else {
                        peekView?.visibility = View.VISIBLE
                    }
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Logger.debug(TAG, "onStateChanged() -> $newState")
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
        messagesView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        messagesView?.adapter = MessagesAdapter()
    }

    /**
     * [ChatBotView] implementation
     */

    override fun hideLoadingIndicator() {
        progressView?.hide()
    }

    override fun showLoadingIndicator() {
        progressView?.show()
    }

    override fun showResponses(nestables: List<Nestable>) {
        adapter?.submitList(nestables)
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
                    presenter?.onResetDataRequested()
                } else {
                    responsesView?.smoothScrollToPosition(0)
                }
            }
        }
    }

}