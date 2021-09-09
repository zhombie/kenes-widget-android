package q19.kenes.widget.ui.presentation.call.text

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EdgeEffect
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kz.q19.domain.model.media.Media
import kz.q19.domain.model.message.Message
import kz.q19.utils.android.dp2Px
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.cinema.model.Movie
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.museum.model.Painting
import kz.zhombie.radio.Radio
import kz.zhombie.radio.getDurationOrZeroIfUnset
import kz.zhombie.radio.getPositionByProgress
import q19.kenes.widget.core.logging.Logger
import q19.kenes.widget.domain.model.sourceUri
import q19.kenes.widget.ui.components.KenesMessageInputView
import q19.kenes.widget.ui.components.KenesToolbar
import q19.kenes.widget.ui.presentation.CoilImageLoader
import q19.kenes.widget.ui.presentation.common.chat.ChatMessagesAdapter
import q19.kenes.widget.ui.presentation.common.recycler_view.SpacingItemDecoration
import q19.kenes.widget.ui.presentation.platform.BaseFragment
import q19.kenes.widget.util.bindAutoClearedValue
import q19.kenes_widget.R

internal class TextChatFragment : BaseFragment<TextChatPresenter>(R.layout.kenes_fragment_text_chat),
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
    private var showVideoCallScreenButton: MaterialButton? = null
    private var messagesView: RecyclerView? = null
    private var messageInputView: KenesMessageInputView? = null

    // RecyclerView adapter
    private var chatMessagesAdapter: ChatMessagesAdapter? = null

    private val chatMessagesAdapterDataObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                messagesView?.smoothScrollToPosition(0)
            }
        }
    }

    private var imageLoader by bindAutoClearedValue<CoilImageLoader>()

    private var radio: Radio? = null

    // Activity + Fragment communication
    private var listener: Listener? = null

    interface Listener {
        fun onViewReady()
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

        imageLoader = CoilImageLoader(requireContext())

        presenter.attachView(this)
    }

    override fun createPresenter(): TextChatPresenter {
        return injection.provideTextChatPresenter(getCurrentLanguage())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        showVideoCallScreenButton = view.findViewById(R.id.showVideoCallScreenButton)
        messagesView = view.findViewById(R.id.messagesView)
        messageInputView = view.findViewById(R.id.messageInputView)

        setupToolbar()
        setupVideoCallButton()
        setupMessagesView()

        listener?.onViewReady()
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()

        radio?.release()
        radio = null

        chatMessagesAdapter?.unregisterAdapterDataObserver(chatMessagesAdapterDataObserver)
        chatMessagesAdapter?.callback = null
        chatMessagesAdapter = null
    }

    private fun setupToolbar() {
        toolbar?.showImage(R.drawable.kenes_ic_user)
        toolbar?.setTitle("Имя оператора")
        toolbar?.setSubtitle("Ожидание...")
        toolbar?.reveal()
    }

    private fun setupVideoCallButton() {
        showVideoCallScreenButton?.setOnClickListener {
            listener?.onShowVideoCallScreen()
        }
    }

    private fun setupMessagesView() {
        messagesView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)

        chatMessagesAdapter = ChatMessagesAdapter(this)
        messagesView?.addItemDecoration(SpacingItemDecoration(5F.dp2Px()))
        messagesView?.adapter = chatMessagesAdapter

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

                if (showVideoCallScreenButton?.visibility == View.VISIBLE) {
                    toolbar?.elevation = 0F
                } else {
                    toolbar?.elevation = recyclerView.computeVerticalScrollOffset().toFloat()
                }
            }
        })

        messageInputView?.setSelectAttachmentButtonEnabled(true)

        messageInputView?.setOnSelectAttachmentClickListener {
            // TODO: Implement media selection with certain permissions
        }

        messageInputView?.setOnSendMessageClickListener { _, message ->
            presenter.onSendTextMessage(message)
        }

        messageInputView?.setOnTextChangedListener { s, _, _, _ ->
            if (s.isNullOrBlank()) {
                messageInputView?.setSendMessageButtonEnabled(false)
            } else {
                messageInputView?.setSendMessageButtonEnabled(true)
            }
        }
    }

    fun showCallAgentInfo(fullName: String, photoUrl: String?) = runOnUiThread {
        toolbar?.setImageContentPadding(0)
        toolbar?.showImage(photoUrl)

        toolbar?.setTitle(fullName)
        toolbar?.setSubtitle("Оператор")

        toolbar?.reveal()
    }

    fun showHangupCallButton() = runOnUiThread {
        if (toolbar?.isRightButtonEnabled() == false) {
            toolbar?.setRightButtonEnabled(true)
            toolbar?.setRightButtonBackgroundTint(R.color.kenes_soft_red)
            toolbar?.setRightButtonIcon(R.drawable.kenes_ic_phone)
            toolbar?.setRightButtonIconTint(R.color.kenes_white)
            toolbar?.setRightButtonOnClickListener {
                listener?.onHangupCall()
            }
        }
    }

    fun hideHangupCallButton() = runOnUiThread {
        toolbar?.setRightButtonOnClickListener(null)
        toolbar?.setRightButtonEnabled(false)
    }

    fun showVideoCallScreenSwitcher() = runOnUiThread {
        showVideoCallScreenButton?.visibility = View.VISIBLE
    }

    fun hideVideoCallScreenSwitcher() = runOnUiThread {
        showVideoCallScreenButton?.visibility = View.GONE
    }

    fun onNewChatMessage(message: Message) = runOnUiThread {
        presenter.onNewChatMessage(message)
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
            .setFooterViewEnabled(true)
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
            .setFooterViewEnabled(true)
            .showSafely(childFragmentManager)
    }

    override fun onVideoClicked(imageView: ImageView, media: Media) {
        CinemaDialogFragment.Builder()
            .setMovie(Movie(Uri.parse(media.urlPath), Movie.Info(media.title)))
            .setScreenView(imageView)
            .setFooterViewEnabled(true)
            .showSafely(childFragmentManager)
    }

    override fun onAudioClicked(media: Media, itemPosition: Int) {
        Logger.debug(TAG, "onAudioClicked() -> $media, $itemPosition")

        val uri = media.sourceUri ?: return

        radio?.let {
            if (uri == it.currentSource && !it.isReleased()) {
                it.playOrPause()
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
                        Radio.PlaybackState.READY -> {
                            chatMessagesAdapter?.resetAudioPlaybackState(
                                itemPosition = itemPosition,
                                duration = getAudioDuration()
                            )
                        }
                        Radio.PlaybackState.ENDED -> {
                            chatMessagesAdapter?.resetAudioPlaybackState(
                                itemPosition = itemPosition,
                                duration = getAudioDuration()
                            )

                            radio?.release()
                            radio = null
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

                    chatMessagesAdapter?.resetAudioPlaybackState(itemPosition, getAudioDuration())

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
     * [TextChatView] implementation
     */

    override fun showNewMessage(message: Message) = runOnUiThread {
        chatMessagesAdapter?.addNewMessage(message)
    }

    override fun clearMessageInput() = runOnUiThread {
        messageInputView?.clearInputViewText()
    }

}