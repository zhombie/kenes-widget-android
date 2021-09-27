package kz.q19.kenes.widget.ui.presentation.home

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EdgeEffect
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.knowledge_base.Element
import kz.q19.domain.model.knowledge_base.Nestable
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.kenes.widget.R
import kz.q19.kenes.widget.api.ImageLoader
import kz.q19.kenes.widget.core.Settings
import kz.q19.kenes.widget.core.logging.Logger
import kz.q19.kenes.widget.domain.model.sourceUri
import kz.q19.kenes.widget.ui.components.KenesMessageInputView
import kz.q19.kenes.widget.ui.components.KenesProgressView
import kz.q19.kenes.widget.ui.presentation.HomeScreenDelegate
import kz.q19.kenes.widget.ui.presentation.common.BottomSheetState
import kz.q19.kenes.widget.ui.presentation.common.HomeFragment
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import kz.q19.kenes.widget.ui.presentation.common.chat.ChatMessagesHeaderAdapter
import kz.q19.kenes.widget.ui.presentation.common.recycler_view.SpacingItemDecoration
import kz.q19.kenes.widget.util.AlertDialogBuilder
import kz.q19.kenes.widget.util.bindAutoClearedValue
import kz.q19.kenes.widget.util.hideKeyboardCompat
import kz.q19.utils.android.clipboardManager
import kz.q19.utils.android.dp2Px
import kz.q19.utils.html.HTMLCompat
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.cinema.model.Movie
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.museum.model.Painting
import kz.zhombie.radio.Radio
import kz.zhombie.radio.getDurationOrZeroIfUnset
import kz.zhombie.radio.getPositionByProgress
import kotlin.math.roundToInt

internal class ChatbotFragment : HomeFragment<ChatbotPresenter>(R.layout.kenes_fragment_chatbot),
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

    private val chatMessagesAdapterDataObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                messagesView?.smoothScrollToPosition(0)
            }
        }
    }

    // CoordinatorLayout + BottomSheet
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var bottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback? = null

    private var imageLoader by bindAutoClearedValue<ImageLoader>()

    private var radio: Radio? = null

    // onBackPressed() dispatcher for Fragment
    private var onBackPressedCallback: OnBackPressedCallback? = null

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

        imageLoader = Settings.getImageLoader()

        presenter.attachView(this)
    }

    override fun createPresenter(): ChatbotPresenter {
        return injection.provideChatbotPresenter(getCurrentLanguage())
    }

    override fun onResume() {
        super.onResume()

        Logger.debug(TAG, "onResume()")

        if (onBackPressedCallback == null) {
            onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback(this) {
                if (presenter.onBackPressed()) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        } else {
            onBackPressedCallback?.isEnabled = true
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

        onBackPressedCallback?.isEnabled = false
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onDestroyView() {
        radio?.release()
        radio = null

        super.onDestroyView()

        bottomSheetBehaviorCallback?.let { bottomSheetBehavior?.removeBottomSheetCallback(it) }
        bottomSheetBehaviorCallback = null
        bottomSheetBehavior = null

        responseGroupsAdapter?.setCallback(null)
        responseGroupsAdapter = null

        chatMessagesAdapter?.unregisterAdapterDataObserver(chatMessagesAdapterDataObserver)
        chatMessagesAdapter?.let { concatAdapter?.removeAdapter(it) }
        chatMessagesAdapter?.callback = null
        chatMessagesAdapter = null

        chatMessagesHeaderAdapter?.let { concatAdapter?.removeAdapter(it) }
        chatMessagesHeaderAdapter = null

        messagesView?.clearOnScrollListeners()
        responsesView?.clearOnScrollListeners()
    }

    override fun onDestroy() {
        super.onDestroy()

        onBackPressedCallback?.remove()
        onBackPressedCallback = null
    }

    private fun setupResponsesView() {
        responseGroupsAdapter = ResponseGroupsAdapter()
        responseGroupsAdapter?.setCallback(object : ResponseGroupsAdapter.Callback {
            override fun onBackPressed(element: Element) {
                presenter.onBackPressed()
            }

            override fun onMenuButtonClicked() {
                AlertDialogBuilder(requireContext())
                    .setTitle(R.string.kenes_menu)
                    .setItems(
                        arrayOf(
                            getString(R.string.kenes_copy),
                            getString(R.string.kenes_share)
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
        responsesView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
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
                    requireContext().resources.getDimensionPixelOffset(R.dimen.kenes_bottom_sheet_peek_height)

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
//                    Logger.debug(TAG, "onStateChanged() -> $newState")

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
        messagesView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesHeaderAdapter = ChatMessagesHeaderAdapter()
        chatMessagesAdapter = ChatMessagesAdapter(this)
        messagesView?.addItemDecoration(SpacingItemDecoration(5F.dp2Px()))
        concatAdapter = ConcatAdapter(chatMessagesAdapter, chatMessagesHeaderAdapter)
        messagesView?.adapter = concatAdapter

        chatMessagesAdapter?.registerAdapterDataObserver(chatMessagesAdapterDataObserver)

        messagesView?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    color = Color.parseColor("#2667E5")
                }
            }
        }

        messagesView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                listener?.onVerticalScroll(recyclerView.computeVerticalScrollOffset())
            }
        })

        messageInputView?.setSelectMediaButtonEnabled(false)

        messageInputView?.setOnSendMessageClickListener { _, message ->
            presenter.onSendTextMessage(message)
        }

        messageInputView?.setSendMessageButtonEnabled(false)
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

    override fun onImageClicked(imageView: ImageView, media: Media) {
        MuseumDialogFragment.Builder()
            .setPaintingLoader(imageLoader ?: return)
            .setPainting(Painting(Uri.parse(media.urlPath), Painting.Info(media.title)))
            .setImageView(imageView)
            .setFooterViewEnabled(false)
            .showSafely(childFragmentManager)
    }

    override fun onImagesClicked(
        recyclerView: RecyclerView,
        images: List<Media>,
        imagePosition: Int
    ) {
        MuseumDialogFragment.Builder()
            .setPaintingLoader(imageLoader ?: return)
            .setPaintings(images.map { media ->
                Painting(
                    Uri.parse(media.urlPath),
                    Painting.Info(media.title)
                )
            })
            .setStartPosition(imagePosition)
            .setFooterViewEnabled(false)
            .showSafely(childFragmentManager)
    }

    override fun onVideoClicked(imageView: ImageView, media: Media) {
        CinemaDialogFragment.Builder()
            .setMovie(Movie(Uri.parse(media.urlPath), Movie.Info(media.title)))
            .setScreenView(imageView)
            .setFooterViewEnabled(false)
            .showSafely(childFragmentManager)
    }

    override fun onAudioClicked(media: Media, itemPosition: Int) {
        val uri = media.sourceUri ?: return

        if (radio?.isReleased() == false) {
            if (uri == radio?.currentSource) {
                radio?.playOrPause()
                return
            }
        }

        radio?.release()
        radio = null
        radio = Radio.Builder(requireContext())
            .create(object : Radio.Listener {
                private fun getAudioDuration(): Long {
                    return radio?.getDurationOrZeroIfUnset() ?: 0L
                }

                override fun onPlaybackStateChanged(state: Radio.PlaybackState) {
                    Logger.debug(TAG, "onPlaybackStateChanged() -> state: $state")
                    when (state) {
                        Radio.PlaybackState.READY, Radio.PlaybackState.ENDED -> {
                            chatMessagesAdapter?.resetAudioPlaybackState(
                                itemPosition = itemPosition,
                                duration = getAudioDuration()
                            )
                        }
                        else -> {
                        }
                    }
                }

                override fun onIsPlayingStateChanged(isPlaying: Boolean) {
                    Logger.debug(TAG, "onIsPlayingStateChanged() -> isPlaying: $isPlaying")

                    chatMessagesAdapter?.setAudioPlaybackState(itemPosition, isPlaying)
                }

                override fun onPlaybackPositionChanged(position: Long) {
                    Logger.debug(TAG, "onPlaybackPositionChanged() -> position: $position")

                    chatMessagesAdapter?.setAudioPlayProgress(
                        itemPosition = itemPosition,
                        progress = radio?.currentPercentage ?: 0F,
                        currentPosition = position,
                        duration = getAudioDuration()
                    )
                }

                override fun onPlayerError(cause: Throwable?) {
                    radio?.release()
                    radio = null
                    toast("ERROR", Toast.LENGTH_SHORT)
                }
            })

        radio?.start(uri, true)
    }

    override fun onSeekBarChange(media: Media, progress: Int): Boolean {
        if (radio == null) return false
        if (radio?.isReleased() == true) return false
        if (media.sourceUri == radio?.currentSource) {
            val position = radio?.getPositionByProgress(progress)
            if (position != null) {
                radio?.seekTo(position)
                return true
            }
        }
        return false
    }

    override fun onMessageLongClicked(text: String) {
        toast("copy: $text")
    }

    /**
     * [ChatbotView] implementation
     */

    override fun showLoadingIndicator() {
        progressView?.show()
    }

    override fun hideLoadingIndicator() {
        progressView?.hide()
    }

    override fun showResponses(nestables: List<Nestable>) = runOnUiThread {
        responseGroupsAdapter?.submitList(nestables)
    }

    override fun showNewChatMessage(message: Message) = runOnUiThread {
        chatMessagesAdapter?.addNewMessage(message)
    }

    override fun copyHTMLText(label: String, text: CharSequence?, htmlText: String) {
        if (text.isNullOrBlank()) {
            context?.clipboardManager?.setPrimaryClip(
                ClipData.newHtmlText(label, HTMLCompat.fromHtml(htmlText), htmlText)
            )
        } else {
            context?.clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
        }

        toast(R.string.kenes_copy)
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
        toast(R.string.kenes_no_knowledge_base_available_response)
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